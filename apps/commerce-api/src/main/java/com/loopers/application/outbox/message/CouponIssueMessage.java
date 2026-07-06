package com.loopers.application.outbox.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** coupon-issue-requests 메시지(JSON 계약). commerce-api 가 발행하고 소비한다. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CouponIssueMessage(
        String eventId,
        String requestId,
        Long couponId,
        Long userId,
        long occurredAt
) {
}
