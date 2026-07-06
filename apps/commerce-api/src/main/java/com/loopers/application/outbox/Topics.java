package com.loopers.application.outbox;

/**
 * Kafka 토픽 이름.
 * <ul>
 *   <li>catalog-events : 상품/재고/좋아요/조회 이벤트 (key = productId → 상품별 순서 보장)</li>
 *   <li>order-events   : 주문/결제 이벤트 (key = orderId → 주문별 순서 보장)</li>
 *   <li>coupon-issue-requests : 쿠폰 발급 요청 (key = couponId)</li>
 * </ul>
 */
public final class Topics {
    public static final String CATALOG_EVENTS = "catalog-events";
    public static final String ORDER_EVENTS = "order-events";
    public static final String COUPON_ISSUE_REQUESTS = "coupon-issue-requests";

    private Topics() {}
}
