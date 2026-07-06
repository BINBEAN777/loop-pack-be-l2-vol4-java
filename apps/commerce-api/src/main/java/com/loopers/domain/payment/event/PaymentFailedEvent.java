package com.loopers.domain.payment.event;

/**
 * 결제가 실패했다는 "사실"(Event).
 */
public record PaymentFailedEvent(Long paymentId, Long orderId, Long userId, String reason) {
}
