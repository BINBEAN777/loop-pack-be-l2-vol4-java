package com.loopers.domain.metrics;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 상품 지표 집계 결과(읽기 모델). Consumer 가 이벤트를 받아 upsert 한다.
 *
 * <p>같은 상품의 이벤트는 partitionKey=productId 로 항상 같은 파티션→같은 컨슈머 스레드에서
 * 순서대로 처리되므로, 단순 증감으로도 정합성이 유지된다.
 * lastEventAt 은 "최신 이벤트만 반영"(out-of-order 방어) 판단에 쓴다.
 */
@Entity
@Table(name = "product_metrics")
public class ProductMetrics extends BaseEntity {

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Column(name = "like_count", nullable = false)
    private long likeCount;

    @Column(name = "sales_count", nullable = false)
    private long salesCount;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "last_event_at", nullable = false)
    private long lastEventAt;

    protected ProductMetrics() {}

    private ProductMetrics(Long productId) {
        this.productId = productId;
        this.likeCount = 0;
        this.salesCount = 0;
        this.viewCount = 0;
        this.lastEventAt = 0L;
    }

    public static ProductMetrics create(Long productId) {
        return new ProductMetrics(productId);
    }

    public void addLike(long delta, long occurredAt) {
        this.likeCount = Math.max(0, this.likeCount + delta);
        touch(occurredAt);
    }

    public void addSales(long quantity, long occurredAt) {
        this.salesCount += quantity;
        touch(occurredAt);
    }

    public void addView(long occurredAt) {
        this.viewCount++;
        touch(occurredAt);
    }

    private void touch(long occurredAt) {
        if (occurredAt > this.lastEventAt) {
            this.lastEventAt = occurredAt;
        }
    }

    public Long getProductId() { return productId; }
    public long getLikeCount() { return likeCount; }
    public long getSalesCount() { return salesCount; }
    public long getViewCount() { return viewCount; }
    public long getLastEventAt() { return lastEventAt; }
}
