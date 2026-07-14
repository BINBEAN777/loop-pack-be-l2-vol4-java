package com.loopers.application.queue;

import com.loopers.domain.queue.EntryTokenStore;
import com.loopers.domain.queue.WaitingQueueRepository;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "loopers.queue.scheduler.enabled=true",         // 이 테스트만 스케줄러 빈 생성
        "loopers.queue.scheduler.batch-size=2",
        "loopers.queue.scheduler.interval-ms=3600000"   // 자동 실행은 막고 issueTokens() 를 직접 호출
})
class TokenIssueSchedulerTest {

    @Autowired private TokenIssueScheduler scheduler;
    @Autowired private WaitingQueueRepository waitingQueueRepository;
    @Autowired private EntryTokenStore entryTokenStore;
    @Autowired private RedisCleanUp redisCleanUp;

    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();
    }

    @DisplayName("스케줄러 실행 시 대기열 앞 N명이 꺼내져 토큰을 받고, 나머지는 순번이 당겨진다.")
    @Test
    void issueTokens_admitsBatchSize_andShiftsRemaining() {
        // arrange — 3명 대기 (batch-size=2)
        waitingQueueRepository.enter(1L);
        waitingQueueRepository.enter(2L);
        waitingQueueRepository.enter(3L);

        // act — 스케줄 타이밍에 의존하지 않고 메서드를 직접 호출
        scheduler.issueTokens();

        // assert — 앞 2명은 토큰 받고 큐에서 빠짐
        assertThat(entryTokenStore.find(1L)).isPresent();
        assertThat(entryTokenStore.find(2L)).isPresent();
        // 3번째는 아직 토큰 없고, 이제 순번 0 (맨 앞으로 당겨짐)
        assertThat(entryTokenStore.find(3L)).isEmpty();
        assertThat(waitingQueueRepository.findRank(3L)).hasValue(0L);
        assertThat(waitingQueueRepository.size()).isEqualTo(1L);
    }

    @DisplayName("배치 크기보다 많은 인원이 있어도, 한 번 실행에 딱 배치 크기만큼만 빠진다 (처리량 상한).")
    @Test
    void issueTokens_capsAtBatchSize_whenOverloaded() {
        // arrange — 5명 대기 (batch-size=2)
        for (long u = 1; u <= 5; u++) {
            waitingQueueRepository.enter(u);
        }

        // act & assert — 매 실행마다 2명씩만 빠지고, 빈 큐에도 안전
        scheduler.issueTokens();
        assertThat(waitingQueueRepository.size()).isEqualTo(3L);

        scheduler.issueTokens();
        assertThat(waitingQueueRepository.size()).isEqualTo(1L);

        scheduler.issueTokens();
        assertThat(waitingQueueRepository.size()).isEqualTo(0L);

        scheduler.issueTokens();   // 빈 큐 실행에도 예외 없이 안정
        assertThat(waitingQueueRepository.size()).isEqualTo(0L);
    }
}
