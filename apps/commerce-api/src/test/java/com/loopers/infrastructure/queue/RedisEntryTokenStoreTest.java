package com.loopers.infrastructure.queue;

import com.loopers.domain.queue.EntryTokenStore;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@TestPropertySource(properties = "loopers.queue.token.ttl-seconds=1")   // 만료 테스트 위해 TTL 1초로 오버라이드
class RedisEntryTokenStoreTest {

    @Autowired private EntryTokenStore entryTokenStore;
    @Autowired private RedisCleanUp redisCleanUp;

    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();
    }

    @DisplayName("토큰을 발급하면, 발급된 값과 동일한 토큰을 조회할 수 있다.")
    @Test
    void issue_thenFind_returnsSameToken() {
        // act
        String issued = entryTokenStore.issue(1L);

        // assert
        Optional<String> found = entryTokenStore.find(1L);
        assertThat(found).hasValue(issued);
    }

    @DisplayName("토큰을 삭제하면, 더 이상 조회되지 않는다.")
    @Test
    void remove_thenFind_returnsEmpty() {
        // arrange
        entryTokenStore.issue(1L);

        // act
        entryTokenStore.remove(1L);

        // assert
        assertThat(entryTokenStore.find(1L)).isEmpty();
    }

    @DisplayName("TTL 이 지나면 토큰이 자동 만료되어 조회되지 않는다.")
    @Test
    void token_expiresAfterTtl() {
        // arrange
        entryTokenStore.issue(1L);

        // assert — TTL(1초)이 지나면 스스로 사라진다
        await().atMost(Duration.ofSeconds(3))
                .until(() -> entryTokenStore.find(1L).isEmpty());
    }
}
