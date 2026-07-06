package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponIssueRequest;
import com.loopers.domain.coupon.CouponIssueRequestRepository;
import com.loopers.domain.coupon.event.CouponIssueRequestedEvent;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 선착순 쿠폰 발급 "요청" 유즈케이스.
 *
 * <p>API 는 발급을 직접 수행하지 않는다: 요청을 접수(REQUESTED)하고 이벤트를 발행(→ Outbox → Kafka)한 뒤
 * 즉시 requestId 를 돌려준다. 실제 발급은 Consumer 가 처리한다(비동기 분리 → 순간 폭주 흡수).
 */
@RequiredArgsConstructor
@Component
public class CouponIssueFacade {

    private final CouponIssueRequestRepository requestRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public String requestIssue(Long couponId, Long userId) {
        String requestId = UUID.randomUUID().toString();
        // 요청 기록 저장 + Outbox 기록(@EventListener)이 같은 트랜잭션으로 커밋된다.
        requestRepository.save(CouponIssueRequest.create(requestId, couponId, userId));
        eventPublisher.publishEvent(new CouponIssueRequestedEvent(requestId, couponId, userId));
        return requestId;
    }

    @Transactional(readOnly = true)
    public CouponIssueInfo getStatus(String requestId) {
        CouponIssueRequest request = requestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                        "[requestId = " + requestId + "] 발급 요청을 찾을 수 없습니다."));
        return CouponIssueInfo.from(request);
    }
}
