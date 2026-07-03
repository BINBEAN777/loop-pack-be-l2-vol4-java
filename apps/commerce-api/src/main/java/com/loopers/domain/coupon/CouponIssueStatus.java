package com.loopers.domain.coupon;

public enum CouponIssueStatus {
    REQUESTED,   // 발급 요청 접수(Kafka 발행 대기/처리 전)
    ISSUED,      // 발급 완료
    FAILED       // 발급 실패(수량 소진/중복 등)
}
