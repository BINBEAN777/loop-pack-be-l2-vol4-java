package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * 선착순 쿠폰 발급 요청. API 는 이 요청만 접수(REQUESTED)하고 즉시 응답하며,
 * Consumer 가 실제 발급을 처리해 상태를 ISSUED/FAILED 로 전이한다. 유저는 requestId 로 결과를 폴링한다.
 */
@Entity
@Table(name = "coupon_issue_request")
public class CouponIssueRequest extends BaseEntity {

    @Column(name = "request_id", nullable = false, unique = true, length = 36)
    private String requestId;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponIssueStatus status;

    @Column(length = 100)
    private String reason;

    protected CouponIssueRequest() {}

    private CouponIssueRequest(String requestId, Long couponId, Long userId) {
        this.requestId = requestId;
        this.couponId = couponId;
        this.userId = userId;
        this.status = CouponIssueStatus.REQUESTED;
    }

    public static CouponIssueRequest create(String requestId, Long couponId, Long userId) {
        return new CouponIssueRequest(requestId, couponId, userId);
    }

    public void markIssued() {
        this.status = CouponIssueStatus.ISSUED;
    }

    public void markFailed(String reason) {
        this.status = CouponIssueStatus.FAILED;
        this.reason = reason;
    }

    public boolean isPending() {
        return this.status == CouponIssueStatus.REQUESTED;
    }

    public String getRequestId() { return requestId; }
    public Long getCouponId() { return couponId; }
    public Long getUserId() { return userId; }
    public CouponIssueStatus getStatus() { return status; }
    public String getReason() { return reason; }
}
