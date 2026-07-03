package com.loopers.application.outbox.message;

/**
 * catalog-events 토픽 메시지(JSON 계약). 소비자(commerce-streamer)가 동일 필드로 역직렬화한다.
 * eventType: LIKE_ADDED | LIKE_REMOVED | PRODUCT_VIEWED
 */
public record CatalogEventMessage(
        String eventId,
        String eventType,
        Long productId,
        long occurredAt
) {
}
