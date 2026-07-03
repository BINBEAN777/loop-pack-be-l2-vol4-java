package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponIssueFacade;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 선착순 쿠폰 발급 요청/조회 API.
 * - POST /coupons/{couponId}/issue-requests : 발급 요청 접수(비동기) → requestId 반환
 * - GET  /coupon-issue-requests/{requestId} : 발급 결과 폴링
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class CouponIssueV1Controller {

    private final CouponIssueFacade couponIssueFacade;

    @PostMapping("/coupons/{couponId}/issue-requests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<CouponIssueV1Dto.IssueRequestResponse> request(
            @RequestHeader(value = "X-Loopers-UserId", required = false) Long userId,
            @PathVariable Long couponId
    ) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "X-Loopers-UserId 헤더가 필요합니다.");
        }
        String requestId = couponIssueFacade.requestIssue(couponId, userId);
        return ApiResponse.success(CouponIssueV1Dto.IssueRequestResponse.accepted(requestId));
    }

    @GetMapping("/coupon-issue-requests/{requestId}")
    public ApiResponse<CouponIssueV1Dto.IssueStatusResponse> status(@PathVariable String requestId) {
        return ApiResponse.success(
                CouponIssueV1Dto.IssueStatusResponse.from(couponIssueFacade.getStatus(requestId)));
    }
}
