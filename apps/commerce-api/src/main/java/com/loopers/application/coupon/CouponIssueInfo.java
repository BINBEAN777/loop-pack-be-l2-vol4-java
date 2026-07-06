package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponIssueRequest;
import com.loopers.domain.coupon.CouponIssueStatus;

/** 발급 요청 상태 조회(폴링) 결과. */
public record CouponIssueInfo(String requestId, CouponIssueStatus status, String reason) {

    public static CouponIssueInfo from(CouponIssueRequest request) {
        return new CouponIssueInfo(request.getRequestId(), request.getStatus(), request.getReason());
    }
}
