package com.loopers.application.payment;

import com.loopers.domain.payment.event.PaymentCompletedEvent;
import com.loopers.domain.payment.event.PaymentFailedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 결제 결과 알림 발송 — 결제 트랜잭션의 부가 로직.
 *
 * <p>알림은 외부 연동(느리고/실패 가능)이므로 {@code @Async} 로 결제 응답 지연과 분리한다.
 * {@code AFTER_COMMIT} 이라 결제가 확정된 뒤에만 발송하며, 알림 실패가 결제를 되돌리지 않는다.
 * (실제 발송 연동 대신 로깅으로 대체 — 경계 분리가 학습 포인트)
 */
@Slf4j
@Component
public class PaymentNotificationListener {

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCompleted(PaymentCompletedEvent e) {
        log.info("[알림] 결제 완료 안내 발송 userId={} orderId={}", e.userId(), e.orderId());
    }

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentFailed(PaymentFailedEvent e) {
        log.info("[알림] 결제 실패 안내 발송 userId={} orderId={} reason={}",
                e.userId(), e.orderId(), e.reason());
    }
}
