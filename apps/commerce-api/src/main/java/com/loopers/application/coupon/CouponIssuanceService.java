package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponIssueRequest;
import com.loopers.domain.coupon.CouponIssueRequestRepository;
import com.loopers.domain.coupon.CouponModel;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.coupon.CouponStockRepository;
import com.loopers.domain.coupon.UserCouponRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 선착순 쿠폰 실제 발급 처리 — Consumer 가 호출한다.
 *
 * <p>동시성 제어: 재고 차감을 원자적 조건부 UPDATE({@code issueOne}) 로 처리해 <b>초과 발급을 원천 차단</b>한다.
 * <p>멱등/중복 방지: 이미 처리된 요청(REQUESTED 아님)은 건너뛰고, 같은 유저의 중복 발급도 막는다.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class CouponIssuanceService {

    private final CouponIssueRequestRepository requestRepository;
    private final CouponStockRepository couponStockRepository;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    @Transactional
    public void issue(String requestId, Long couponId, Long userId) {
        CouponIssueRequest request = requestRepository.findByRequestId(requestId).orElse(null);
        if (request == null) {
            log.warn("[쿠폰발급] 알 수 없는 requestId={}", requestId);
            return;
        }
        if (!request.isPending()) {
            return;   // 이미 처리됨(ISSUED/FAILED) → 멱등하게 무시
        }

        // 중복 발급 방지 (같은 유저, 같은 쿠폰)
        if (userCouponRepository.existsByCouponIdAndUserId(couponId, userId)) {
            request.markFailed("ALREADY_ISSUED");
            return;
        }

        // 선착순 수량 제한 — 원자적 차감 (남아있을 때만 성공)
        int acquired = couponStockRepository.issueOne(couponId);
        if (acquired == 0) {
            request.markFailed("SOLD_OUT");
            return;
        }

        CouponModel coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                        "[id = " + couponId + "] 쿠폰을 찾을 수 없습니다."));
        userCouponRepository.save(coupon.issue(userId));
        request.markIssued();
    }
}
