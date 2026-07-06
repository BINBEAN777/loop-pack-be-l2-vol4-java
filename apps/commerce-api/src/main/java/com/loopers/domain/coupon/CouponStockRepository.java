package com.loopers.domain.coupon;

import java.util.Optional;

public interface CouponStockRepository {

    CouponStock save(CouponStock stock);

    Optional<CouponStock> findByCouponId(Long couponId);

    /**
     * 재고가 남아있으면 발급 수를 원자적으로 1 증가시킨다.
     * @return 성공(발급 확보) 시 1, 소진 시 0
     */
    int issueOne(Long couponId);
}
