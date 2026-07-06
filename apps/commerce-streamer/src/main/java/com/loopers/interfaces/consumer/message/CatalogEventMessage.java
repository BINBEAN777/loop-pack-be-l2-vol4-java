package com.loopers.interfaces.consumer.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** catalog-events 메시지(JSON 계약, producer 와 필드 일치). eventType: LIKE_ADDED | LIKE_REMOVED | PRODUCT_VIEWED */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogEventMessage(
        String eventId,
        String eventType,
        Long productId,
        long occurredAt
) {
}
