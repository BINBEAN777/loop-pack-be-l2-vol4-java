package com.loopers.domain.like.event;

/**
 * 좋아요가 취소되었다는 "사실"(Event).
 */
public record LikeRemovedEvent(Long userId, Long productId) {
}
