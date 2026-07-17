package com.loopers.application.ranking;

import com.loopers.config.redis.RedisConfig;
import com.loopers.domain.ranking.RankingKey;
import com.loopers.interfaces.consumer.message.CatalogEventMessage;
import com.loopers.interfaces.consumer.message.OrderEventMessage;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
class RankingEventHandlerTest {

    @Autowired private RankingEventHandler handler;
    @Autowired private RedisCleanUp redisCleanUp;

    @Autowired
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private RedisTemplate<String, String> redisTemplate;

    private static final Long PRODUCT_ID = 100L;
    private static final LocalDate DATE = LocalDate.of(2026, 7, 16);
    private static final String KEY = "ranking:all:20260716";

    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();
    }

    private long occurredAtOn(LocalDate date) {
        return ZonedDateTime.of(date.atTime(12, 0), RankingKey.ZONE).toInstant().toEpochMilli();
    }

    @DisplayName("조회 이벤트를 처리하면 발생일 랭킹판에 view 가중치만큼 점수가 쌓인다.")
    @Test
    void catalogView_accumulatesWeightedScore() {
        // arrange
        long occurredAt = occurredAtOn(DATE);

        // act
        handler.handleCatalog(List.of(
                new CatalogEventMessage("evt-1", "PRODUCT_VIEWED", PRODUCT_ID, occurredAt),
                new CatalogEventMessage("evt-2", "PRODUCT_VIEWED", PRODUCT_ID, occurredAt)
        ));

        // assert — view 0.1 × 2건
        Double score = redisTemplate.opsForZSet().score(KEY, String.valueOf(PRODUCT_ID));
        assertThat(score).isCloseTo(0.2, within(1e-9));
    }

    @DisplayName("주문 1건(수량 1)이 좋아요 3건보다 랭킹에서 위다. (가중치 검증)")
    @Test
    void oneOrder_ranksAboveThreeLikes() {
        // arrange
        long occurredAt = occurredAtOn(DATE);
        Long likedProduct = 200L;

        // act — 200번 상품엔 좋아요 3건, 100번 상품엔 주문 1건(수량 1)
        handler.handleCatalog(List.of(
                new CatalogEventMessage("evt-1", "LIKE_ADDED", likedProduct, occurredAt),
                new CatalogEventMessage("evt-2", "LIKE_ADDED", likedProduct, occurredAt),
                new CatalogEventMessage("evt-3", "LIKE_ADDED", likedProduct, occurredAt)
        ));
        handler.handleOrder(List.of(
                new OrderEventMessage("evt-4", "ORDER_PLACED", 1L, 7L, occurredAt,
                        List.of(new OrderEventMessage.Line(PRODUCT_ID, 1)))
        ));

        // assert — 주문 상품이 0위(1등)
        Long orderRank = redisTemplate.opsForZSet().reverseRank(KEY, String.valueOf(PRODUCT_ID));
        Long likeRank = redisTemplate.opsForZSet().reverseRank(KEY, String.valueOf(likedProduct));
        assertThat(orderRank).isLessThan(likeRank);
    }

    @DisplayName("좋아요 후 취소하면 점수가 0 으로 돌아온다.")
    @Test
    void likeThenUnlike_returnsToZero() {
        long occurredAt = occurredAtOn(DATE);

        handler.handleCatalog(List.of(
                new CatalogEventMessage("evt-1", "LIKE_ADDED", PRODUCT_ID, occurredAt),
                new CatalogEventMessage("evt-2", "LIKE_REMOVED", PRODUCT_ID, occurredAt)
        ));

        Double score = redisTemplate.opsForZSet().score(KEY, String.valueOf(PRODUCT_ID));
        assertThat(score).isCloseTo(0.0, within(1e-9));
    }

    @DisplayName("발생 날짜가 다른 이벤트는 서로 다른 날짜 키에 쌓인다.")
    @Test
    void eventsOnDifferentDates_goToDifferentKeys() {
        // arrange — 16일 조회 1건, 17일 조회 1건
        handler.handleCatalog(List.of(
                new CatalogEventMessage("evt-1", "PRODUCT_VIEWED", PRODUCT_ID, occurredAtOn(DATE)),
                new CatalogEventMessage("evt-2", "PRODUCT_VIEWED", PRODUCT_ID, occurredAtOn(DATE.plusDays(1)))
        ));

        // assert — 각 날짜 판에 0.1 씩
        assertThat(redisTemplate.opsForZSet().score(KEY, String.valueOf(PRODUCT_ID)))
                .isCloseTo(0.1, within(1e-9));
        assertThat(redisTemplate.opsForZSet().score("ranking:all:20260717", String.valueOf(PRODUCT_ID)))
                .isCloseTo(0.1, within(1e-9));
    }

    @DisplayName("랭킹판이 처음 만들어질 때 TTL(2일)이 걸린다.")
    @Test
    void newRankingKey_hasTwoDayTtl() {
        handler.handleCatalog(List.of(
                new CatalogEventMessage("evt-1", "PRODUCT_VIEWED", PRODUCT_ID, occurredAtOn(DATE))
        ));

        Long ttlSeconds = redisTemplate.getExpire(KEY);
        assertThat(ttlSeconds).isGreaterThan(0).isLessThanOrEqualTo(2 * 24 * 3600L);
    }
}
