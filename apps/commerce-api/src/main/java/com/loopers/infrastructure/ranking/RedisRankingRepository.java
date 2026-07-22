package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankedProduct;
import com.loopers.domain.ranking.RankingKey;
import com.loopers.domain.ranking.RankingRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class RedisRankingRepository implements RankingRepository {

    // 랭킹은 복제 지연을 허용하는 근사치 조회이므로 기본(REPLICA_PREFERRED) 템플릿을 쓴다.
    // (대기열이 master 를 고집했던 것과 대비 — 순번은 "정확"해야 하지만 랭킹은 "몇 초 전"이어도 된다)
    private final RedisTemplate<String, String> redisTemplate;

    public RedisRankingRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<RankedProduct> findPage(RankingKey key, long offset, long count) {
        if (count <= 0) {
            return List.of();
        }
        // ZREVRANGE key offset (offset+count-1) WITHSCORES — 점수 내림차순 구간 조회
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(key.value(), offset, offset + count - 1);
        if (tuples == null) {
            return List.of();
        }
        return tuples.stream()
                .filter(t -> t.getValue() != null && t.getScore() != null)
                .map(t -> new RankedProduct(Long.valueOf(t.getValue()), t.getScore()))
                .toList();
    }

    @Override
    public Optional<Long> findRank(RankingKey key, Long productId) {
        // ZREVRANK: 점수 내림차순에서 몇 번째인지(0-based). 멤버가 없으면 null
        Long rank = redisTemplate.opsForZSet().reverseRank(key.value(), String.valueOf(productId));
        return Optional.ofNullable(rank);
    }

    @Override
    public long countMembers(RankingKey key) {
        Long count = redisTemplate.opsForZSet().zCard(key.value());
        return count == null ? 0L : count;
    }
}
