package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.CouponStock;
import com.loopers.domain.coupon.CouponStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class CouponStockRepositoryImpl implements CouponStockRepository {

    private final CouponStockJpaRepository jpaRepository;

    @Override
    public CouponStock save(CouponStock stock) {
        return jpaRepository.save(stock);
    }

    @Override
    public Optional<CouponStock> findByCouponId(Long couponId) {
        return jpaRepository.findByCouponId(couponId);
    }

    @Override
    public int issueOne(Long couponId) {
        return jpaRepository.issueOne(couponId);
    }
}
