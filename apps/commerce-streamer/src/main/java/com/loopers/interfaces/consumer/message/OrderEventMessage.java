package com.loopers.interfaces.consumer.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** order-events 메시지(JSON 계약, producer 와 필드 일치). eventType: ORDER_PLACED */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderEventMessage(
        String eventId,
        String eventType,
        Long orderId,
        Long userId,
        long occurredAt,
        List<Line> lines
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Line(Long productId, int quantity) {
    }
}
