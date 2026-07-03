package com.loopers.application.like;

import com.loopers.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 좋아요 수(like_count) 집계의 "트랜잭션 작업"만 담는 빈.
 *
 * <p>리스너(@Async)와 트랜잭션(@Transactional)을 한 메서드에 겹치면 인터셉터 적용 순서가 모호해진다.
 * 그래서 "언제/어떻게 실행할지"(리스너)와 "무엇을 트랜잭션으로 처리할지"(이 빈)를 분리한다.
 * 비동기 스레드에는 바인딩된 트랜잭션이 없으므로 여기서 새 트랜잭션이 깔끔하게 열린다.
 */
@RequiredArgsConstructor
@Component
public class ProductLikeCounter {

    private final ProductRepository productRepository;

    @Transactional
    public void increase(Long productId) {
        productRepository.increaseLikeCount(productId);   // 원자적 UPDATE (동시 증가에도 안전)
    }

    @Transactional
    public void decrease(Long productId) {
        productRepository.decreaseLikeCount(productId);
    }
}
