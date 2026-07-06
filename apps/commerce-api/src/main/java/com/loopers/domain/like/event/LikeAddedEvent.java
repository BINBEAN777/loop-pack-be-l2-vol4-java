package com.loopers.domain.like.event;

/**
 * 좋아요가 등록되었다는 "사실"(Event). 이미 일어난 일이므로 과거형으로 명명한다.
 * 좋아요 수 집계, 유저 행동 로깅 등 부가 로직이 이 이벤트를 구독한다.
 */
public record LikeAddedEvent(Long userId, Long productId) {
}
