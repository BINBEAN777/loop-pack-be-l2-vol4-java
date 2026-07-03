package com.loopers.application.coupon;

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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CouponIssueFlowTest {

    @Autowired private CouponIssueFacade couponIssueFacade;
    @Autowired private CouponIssuanceService issuanceService;
    @Autowired private CouponRepository couponRepository;
    @Autowired private CouponStockRepository couponStockRepository;
    @Autowired private UserCouponRepository userCouponRepository;
    @Autowired private DatabaseCleanUp databaseCleanUp;

    private static final Long USER_ID = 1L;
    private Long couponId;

    @BeforeEach
    void setUp() {
        DiscountPolicy policy = DiscountPolicy.of(DiscountType.FIXED, 1000L, Money.of(0L));
        couponId = couponRepository.save(
                CouponModel.create("선착순쿠폰", policy, ZonedDateTime.now().plusDays(7))).getId();
        couponStockRepository.save(CouponStock.create(couponId, 10));
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("발급 요청 접수 직후엔 REQUESTED, Consumer 처리 후엔 ISSUED 로 폴링된다.")
    @Test
    void request_thenIssue_pollsIssued() {
        String requestId = couponIssueFacade.requestIssue(couponId, USER_ID);
        assertThat(couponIssueFacade.getStatus(requestId).status()).isEqualTo(CouponIssueStatus.REQUESTED);

        issuanceService.issue(requestId, couponId, USER_ID);   // Consumer 가 하는 일

        assertThat(couponIssueFacade.getStatus(requestId).status()).isEqualTo(CouponIssueStatus.ISSUED);
        assertThat(userCouponRepository.existsByCouponIdAndUserId(couponId, USER_ID)).isTrue();
    }

    @DisplayName("같은 요청을 두 번 처리해도 재고는 한 번만 차감된다. (멱등)")
    @Test
    void duplicateIssue_isIdempotent() {
        String requestId = couponIssueFacade.requestIssue(couponId, USER_ID);

        issuanceService.issue(requestId, couponId, USER_ID);
        issuanceService.issue(requestId, couponId, USER_ID);   // 중복 처리

        CouponStock stock = couponStockRepository.findByCouponId(couponId).orElseThrow();
        assertThat(stock.getIssuedQuantity()).isEqualTo(1);    // 2가 아니라 1
    }
}
