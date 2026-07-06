package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 선착순 쿠폰 재고. 발급 가능 수량을 카운터로 관리한다(couponId 당 1행).
 *
 * <p>실제 차감은 {@code CouponStockRepository.issueOne} 의 원자적 조건부 UPDATE 로 수행한다
 * (issued_quantity &lt; total_quantity 인 경우에만 +1). 동시 요청이 몰려도 초과 발급이 없다.
 */
@Entity
@Table(name = "coupon_stock")
public class CouponStock extends BaseEntity {

    @Column(name = "coupon_id", nullable = false, unique = true)
    private Long couponId;

    @Column(name = "total_quantity", nullable = false)
    private long totalQuantity;

    @Column(name = "issued_quantity", nullable = false)
    private long issuedQuantity;

    protected CouponStock() {}

    private CouponStock(Long couponId, long totalQuantity) {
        if (couponId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰은 필수입니다.");
        }
        if (totalQuantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "발급 수량은 1 이상이어야 합니다.");
        }
        this.couponId = couponId;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = 0;
    }

    public static CouponStock create(Long couponId, long totalQuantity) {
        return new CouponStock(couponId, totalQuantity);
    }

    public Long getCouponId() { return couponId; }
    public long getTotalQuantity() { return totalQuantity; }
    public long getIssuedQuantity() { return issuedQuantity; }
    public long getRemaining() { return totalQuantity - issuedQuantity; }
}
