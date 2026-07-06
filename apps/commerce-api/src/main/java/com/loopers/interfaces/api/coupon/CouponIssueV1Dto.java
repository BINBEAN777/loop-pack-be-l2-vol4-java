package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponIssueInfo;

public final class CouponIssueV1Dto {

    /** 발급 요청 접수 응답 (즉시 반환, 실제 발급은 비동기). */
    public record IssueRequestResponse(String requestId, String status) {
        public static IssueRequestResponse accepted(String requestId) {
            return new IssueRequestResponse(requestId, "REQUESTED");
        }
    }

    /** 발급 결과 폴링 응답. */
    public record IssueStatusResponse(String requestId, String status, String reason) {
        public static IssueStatusResponse from(CouponIssueInfo info) {
            return new IssueStatusResponse(info.requestId(), info.status().name(), info.reason());
        }
    }

    private CouponIssueV1Dto() {}
}
