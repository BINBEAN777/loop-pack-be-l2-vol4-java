package com.loopers.domain.order.event;

/**
 * 주문이 생성되었다는 "사실"(Event). 유저 행동 로깅 등 부가 로직이 구독한다.
 */
public record OrderPlacedEvent(Long orderId, Long userId, long finalAmount) {
}
