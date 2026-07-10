package com.loopers.infrastructure.queue;

import com.loopers.config.redis.RedisConfig;
import com.loopers.domain.queue.EntryTokenStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RedisEntryTokenStore implements EntryTokenStore {

    private static final String TOKEN_KEY_PREFIX = "queue:token:";

    private final RedisTemplate<String, String> redisTemplate;
    private final Duration ttl;

    public RedisEntryTokenStore(
            @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER) RedisTemplate<String, String> redisTemplate,
            @Value("${loopers.queue.token.ttl-seconds:300}") long ttlSeconds   // 기본 5분, 오버라이드 가능
    ) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    @Override
    public String issue(Long userId) {
        String token = UUID.randomUUID().toString();
        // SET queue:token:{userId} {token} EX {ttl}  → TTL 지나면 Redis 가 알아서 삭제
        redisTemplate.opsForValue().set(key(userId), token, ttl);
        return token;
    }

    @Override
    public Optional<String> find(Long userId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key(userId)));
    }

    @Override
    public void remove(Long userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(Long userId) {
        return TOKEN_KEY_PREFIX + userId;
    }
}
