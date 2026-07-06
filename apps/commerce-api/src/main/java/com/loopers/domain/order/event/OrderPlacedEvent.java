package com.loopers.domain.order.event;

import java.util.List;

/**
 * 주문이 생성되었다는 "사실"(Event). 유저 행동 로깅·판매량 집계 등 부가 로직이 구독한다.
 * 판매량 집계를 위해 주문 라인(상품/수량)을 함께 담는다.
 */
public record OrderPlacedEvent(Long orderId, Long userId, long finalAmount, List<Line> lines) {

    public record Line(Long productId, int quantity) {
    }
}
