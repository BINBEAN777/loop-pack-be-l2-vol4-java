package com.loopers.domain.coupon.event;

/**
 * 선착순 쿠폰 발급이 "요청되었다"는 사실. Outbox 를 거쳐 coupon-issue-requests 로 발행된다.
 */
public record CouponIssueRequestedEvent(String requestId, Long couponId, Long userId) {
}
