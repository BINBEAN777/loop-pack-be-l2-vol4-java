package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponIssueRequest;
import com.loopers.domain.coupon.CouponIssueRequestRepository;
import com.loopers.domain.coupon.CouponIssueStatus;
import com.loopers.domain.coupon.CouponModel;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.coupon.CouponStock;
import com.loopers.domain.coupon.CouponStockRepository;
import com.loopers.domain.coupon.DiscountPolicy;
import com.loopers.domain.coupon.DiscountType;
import com.loopers.domain.coupon.UserCouponRepository;
import com.loopers.domain.vo.Money;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.ZonedDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 선착순 발급 동시성 검증 — 100장 한정 쿠폰에 200명이 동시에 요청해도 발급은 정확히 100건이어야 한다.
 * (Consumer 가 호출하는 발급 처리 로직을 직접 병렬 호출해 검증한다)
 */
@SpringBootTest
class CouponIssueConcurrencyTest {

    @Autowired private CouponIssuanceService issuanceService;
    @Autowired private CouponRepository couponRepository;
    @Autowired private CouponStockRepository couponStockRepository;
    @Autowired private CouponIssueRequestRepository requestRepository;
    @Autowired private UserCouponRepository userCouponRepository;
    @Autowired private DatabaseCleanUp databaseCleanUp;

    private static final int LIMIT = 100;
    private static final int REQUESTS = 200;

    private Long couponId;

    @BeforeEach
    void setUp() {
        DiscountPolicy policy = DiscountPolicy.of(DiscountType.FIXED, 1000L, Money.of(0L));
        couponId = couponRepository.save(
                CouponModel.create("선착순쿠폰", policy, ZonedDateTime.now().plusDays(7))).getId();
        couponStockRepository.save(CouponStock.create(couponId, LIMIT));
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("100장 한정에 200명이 동시에 발급 요청해도 정확히 100건만 발급된다.")
    @Test
    void firstComeLimit_neverOverIssues() throws InterruptedException {
        // 요청 200건 미리 접수 (서로 다른 유저)
        for (int i = 0; i < REQUESTS; i++) {
            requestRepository.save(CouponIssueRequest.create("req-" + i, couponId, (long) i));
        }

        ExecutorService executor = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(REQUESTS);
        for (int i = 0; i < REQUESTS; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    issuanceService.issue("req-" + idx, couponId, (long) idx);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        long issued = countIssued();
        CouponStock stock = couponStockRepository.findByCouponId(couponId).orElseThrow();

        assertThat(stock.getIssuedQuantity()).isEqualTo(LIMIT);   // 재고 차감이 정확히 100
        assertThat(issued).isEqualTo(LIMIT);                      // ISSUED 상태 요청도 정확히 100
    }

    private long countIssued() {
        long count = 0;
        for (int i = 0; i < REQUESTS; i++) {
            CouponIssueRequest r = requestRepository.findByRequestId("req-" + i).orElseThrow();
            if (r.getStatus() == CouponIssueStatus.ISSUED) {
                count++;
            }
        }
        return count;
    }
}
