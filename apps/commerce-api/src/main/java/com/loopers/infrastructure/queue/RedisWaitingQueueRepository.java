package com.loopers.infrastructure.queue;

import com.loopers.config.redis.RedisConfig;
import com.loopers.domain.queue.WaitingQueueRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Repository
public class RedisWaitingQueueRepository implements WaitingQueueRepository {

    private static final String WAITING_QUEUE_KEY = "queue:waiting";

    // 순서(순번)는 단일 기준점에서만 정해야 하므로, 쓰기/읽기 모두 마스터 템플릿을 쓴다.
    // (Replica 로 읽으면 복제 지연 때문에 순번이 뒤처지거나 토큰을 놓칠 수 있다.)
    private final RedisTemplate<String, String> redisTemplate;

    public RedisWaitingQueueRepository(
            @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER) RedisTemplate<String, String> redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void enter(Long userId) {
        // ZADD NX: 이미 대기 중이면 score(진입시각)를 덮어쓰지 않는다 → 최초 순번 유지(중복 진입 무시)
        redisTemplate.opsForZSet()
                .addIfAbsent(WAITING_QUEUE_KEY, String.valueOf(userId), System.currentTimeMillis());
    }

    @Override
    public Optional<Long> findRank(Long userId) {
        // ZRANK: score 오름차순에서 몇 번째인지(0-based). 없으면 null → empty
        Long rank = redisTemplate.opsForZSet().rank(WAITING_QUEUE_KEY, String.valueOf(userId));
        return Optional.ofNullable(rank);
    }

    @Override
    public long size() {
        // ZCARD: 대기열 전체 인원. 키가 없으면 null 이므로 0 으로 보정
        Long count = redisTemplate.opsForZSet().zCard(WAITING_QUEUE_KEY);
        return count == null ? 0L : count;
    }

    @Override
    public List<Long> popMin(int count) {
        // ZPOPMIN: score 가 가장 낮은(=먼저 온) N명을 꺼내면서 동시에 제거한다 (atomic)
        Set<ZSetOperations.TypedTuple<String>> popped =
                redisTemplate.opsForZSet().popMin(WAITING_QUEUE_KEY, count);
        if (popped == null) {
            return List.of();
        }
        return popped.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .filter(Objects::nonNull)
                .map(Long::valueOf)
                .toList();
    }
}
