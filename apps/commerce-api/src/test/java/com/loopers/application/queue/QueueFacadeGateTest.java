package com.loopers.application.queue;

import com.loopers.domain.queue.EntryTokenStore;
import com.loopers.support.error.CoreException;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@TestPropertySource(properties = {
        "loopers.queue.gate.enabled=true",       // 관문 켠 상태로 검증
        "loopers.queue.token.ttl-seconds=1"      // 만료 검증 위해 TTL 1초
})
class QueueFacadeGateTest {

    @Autowired private QueueFacade queueFacade;
    @Autowired private EntryTokenStore entryTokenStore;
    @Autowired private RedisCleanUp redisCleanUp;

    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();
    }

    @DisplayName("유효한 토큰이면 검증을 통과한다.")
    @Test
    void validateEntry_passes_withValidToken() {
        String token = entryTokenStore.issue(1L);
        assertThatCode(() -> queueFacade.validateEntry(1L, token)).doesNotThrowAnyException();
    }

    @DisplayName("토큰이 없으면 예외가 발생한다.")
    @Test
    void validateEntry_throws_withoutToken() {
        assertThatThrownBy(() -> queueFacade.validateEntry(1L, null))
                .isInstanceOf(CoreException.class);
    }

    @DisplayName("토큰이 일치하지 않으면 예외가 발생한다.")
    @Test
    void validateEntry_throws_withMismatchedToken() {
        entryTokenStore.issue(1L);
        assertThatThrownBy(() -> queueFacade.validateEntry(1L, "wrong-token"))
                .isInstanceOf(CoreException.class);
    }

    @DisplayName("토큰을 소진하면 삭제된다.")
    @Test
    void consumeEntry_removesToken() {
        entryTokenStore.issue(1L);
        queueFacade.consumeEntry(1L);
        assertThat(entryTokenStore.find(1L)).isEmpty();
    }

    @DisplayName("토큰이 TTL 만료되면 검증에서 거부된다.")
    @Test
    void validateEntry_throws_afterTokenExpires() {
        String token = entryTokenStore.issue(1L);

        // TTL(1초) 지나면 토큰이 사라져 검증 실패
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                assertThatThrownBy(() -> queueFacade.validateEntry(1L, token))
                        .isInstanceOf(CoreException.class));
    }
}
