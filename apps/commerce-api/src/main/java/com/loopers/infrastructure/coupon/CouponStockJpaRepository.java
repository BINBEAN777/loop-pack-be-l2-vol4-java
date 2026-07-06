package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.CouponStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponStockJpaRepository extends JpaRepository<CouponStock, Long> {

    Optional<CouponStock> findByCouponId(Long couponId);

    /** 선착순 원자적 차감 — 남아있을 때만 +1. lost update / 초과 발급 방지. */
    @Modifying
    @Query("update CouponStock s set s.issuedQuantity = s.issuedQuantity + 1 " +
            "where s.couponId = :couponId and s.issuedQuantity < s.totalQuantity")
    int issueOne(@Param("couponId") Long couponId);
}
