package com.loopers.application.like;

import com.loopers.domain.like.event.LikeAddedEvent;
import com.loopers.domain.like.event.LikeRemovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 좋아요 수(like_count) 집계 — 좋아요 트랜잭션의 부가 로직.
 *
 * <p>{@code AFTER_COMMIT} : 좋아요 저장이 커밋된 뒤에만 집계한다. 따라서 "좋아요는 성공했는데
 * 집계만 실패"는 가능해도, 그 반대(집계는 됐는데 좋아요는 롤백)는 없다.
 *
 * <p>{@code @Async} : 좋아요 트랜잭션이 커넥션을 반납한 뒤 별도 스레드에서 집계한다.
 * (동기 AFTER_COMMIT + REQUIRES_NEW 는 원래 커넥션을 쥔 채 두 번째 커넥션을 요구해
 * 고동시성에서 커넥션 풀 데드락을 유발한다 — 그래서 비동기로 분리.)
 *
 * <p>try/catch : 집계 실패가 좋아요 성공을 되돌리지 못하게 격리한다(eventual consistency).
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class LikeCountAggregationListener {

    private final ProductLikeCounter productLikeCounter;

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLikeAdded(LikeAddedEvent event) {
        try {
            productLikeCounter.increase(event.productId());
        } catch (Exception e) {
            log.warn("[좋아요 집계] 증가 실패 productId={}", event.productId(), e);
        }
    }

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLikeRemoved(LikeRemovedEvent event) {
        try {
            productLikeCounter.decrease(event.productId());
        } catch (Exception e) {
            log.warn("[좋아요 집계] 감소 실패 productId={}", event.productId(), e);
        }
    }
}
