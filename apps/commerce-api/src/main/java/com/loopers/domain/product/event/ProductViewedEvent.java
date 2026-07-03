package com.loopers.domain.product.event;

/**
 * 상품이 조회되었다는 "사실"(Event). 조회 수 집계가 구독한다. (userId 는 비로그인 조회 시 null 가능)
 */
public record ProductViewedEvent(Long productId, Long userId) {
}
