package com.loopers.domain.payment.event;

/**
 * 결제가 성공적으로 완료되었다는 "사실"(Event). 알림 발송, 유저 행동 로깅 등이 구독한다.
 */
public record PaymentCompletedEvent(Long paymentId, Long orderId, Long userId, long amount) {
}
