package com.loopers.application.outbox.message;

import java.util.List;

/**
 * order-events 토픽 메시지(JSON 계약). eventType: ORDER_PLACED
 * 판매량 집계를 위해 주문 라인(상품/수량)을 함께 싣는다.
 */
public record OrderEventMessage(
        String eventId,
        String eventType,
        Long orderId,
        Long userId,
        long occurredAt,
        List<Line> lines
) {
    public record Line(Long productId, int quantity) {
    }
}
