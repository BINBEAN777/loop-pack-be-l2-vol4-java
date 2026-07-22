package com.loopers.infrastructure.ranking;

import com.loopers.config.redis.RedisConfig;
import com.loopers.domain.ranking.RankingKey;
import com.loopers.domain.ranking.RankingRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Map;

@Repository
public class RedisRankingRepository implements RankingRepository {

    /** 일간 랭킹판 보존 기간 — 윈도우(1일)의 2배. 어제 랭킹 조회를 지원하면서 메모리를 회수한다. */
    private static final Duration TTL = Duration.ofDays(2);

    // 쓰기는 항상 마스터로 (replica 는 읽기 전용)
    private final RedisTemplate<String, String> redisTemplate;

    public RedisRankingRepository(
            @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER) RedisTemplate<String, String> redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void incrementScores(RankingKey key, Map<Long, Double> scoreDeltaByProductId) {
        if (scoreDeltaByProductId.isEmpty()) {
            return;
        }
        String keyValue = key.value();
        scoreDeltaByProductId.forEach((productId, delta) ->
                redisTemplate.opsForZSet().incrementScore(keyValue, String.valueOf(productId), delta));

        // ZINCRBY 로 키가 처음 생겼을 때만 TTL 을 건다. (매번 걸면 쓰기가 있을 때마다 만료가 밀린다)
        Long ttlSeconds = redisTemplate.getExpire(keyValue);
        if (ttlSeconds != null && ttlSeconds == -1) {
            redisTemplate.expire(keyValue, TTL);
        }
    }
}
