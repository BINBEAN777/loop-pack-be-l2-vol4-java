package com.loopers.application.activity;

import com.loopers.domain.like.event.LikeAddedEvent;
import com.loopers.domain.like.event.LikeRemovedEvent;
import com.loopers.domain.order.event.OrderPlacedEvent;
import com.loopers.domain.payment.event.PaymentCompletedEvent;
import com.loopers.domain.payment.event.PaymentFailedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 유저 행동에 대한 서버 레벨 로깅 — 여러 도메인 이벤트를 구독하는 부가(cross-cutting) 관심사.
 *
 * <p>{@code AFTER_COMMIT} : "실제로 커밋된 행동"만 로깅한다(롤백된 시도는 로깅하지 않는다).
 * 로깅 실패가 본 로직에 영향을 주지 않도록, 주 트랜잭션과 분리된 지점에서 동작한다.
 */
@Slf4j
@Component
public class UserActivityLogListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLikeAdded(LikeAddedEvent e) {
        log.info("[행동로그] userId={} action=LIKE_ADDED productId={}", e.userId(), e.productId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLikeRemoved(LikeRemovedEvent e) {
        log.info("[행동로그] userId={} action=LIKE_REMOVED productId={}", e.userId(), e.productId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedEvent e) {
        log.info("[행동로그] userId={} action=ORDER_PLACED orderId={} amount={}",
                e.userId(), e.orderId(), e.finalAmount());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCompleted(PaymentCompletedEvent e) {
        log.info("[행동로그] userId={} action=PAYMENT_COMPLETED orderId={} amount={}",
                e.userId(), e.orderId(), e.amount());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentFailed(PaymentFailedEvent e) {
        log.info("[행동로그] userId={} action=PAYMENT_FAILED orderId={} reason={}",
                e.userId(), e.orderId(), e.reason());
    }
}
