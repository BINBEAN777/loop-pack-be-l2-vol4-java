package com.loopers.infrastructure.queue;

import com.loopers.domain.queue.WaitingQueueRepository;
import com.loopers.utils.RedisCleanUp;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RedisWaitingQueueRepositoryTest {

    @Autowired
    private WaitingQueueRepository waitingQueueRepository;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();   // Redis는 테스트 간 데이터가 남으니 매번 비운다
    }

    @DisplayName("대기열에 첫 번째로 진입한 유저의 순번은 0이다.")
    @Test
    void rankIsZero_whenFirstUserEnters() {
        // arrange
        Long userId = 1L;

        // act
        waitingQueueRepository.enter(userId);

        // assert
        Optional<Long> rank = waitingQueueRepository.findRank(userId);
        assertThat(rank).hasValue(0L);
    }

    @DisplayName("먼저 진입한 유저가 나중에 진입한 유저보다 앞 순번을 가진다.")
    @Test
    void earlierUserGetsEarlierRank() {
        // arrange & act
        waitingQueueRepository.enter(1L);
        waitingQueueRepository.enter(2L);

        // assert
        assertThat(waitingQueueRepository.findRank(1L)).hasValue(0L);
        assertThat(waitingQueueRepository.findRank(2L)).hasValue(1L);
    }

    @DisplayName("같은 유저가 다시 진입해도 최초 순번이 유지된다(중복 진입 무시).")
    @Test
    void keepsOriginalRank_whenSameUserEntersAgain() {
        // arrange — 1L 이 먼저 들어오고, 그 뒤 2L 이 들어와 자리를 잡는다
        waitingQueueRepository.enter(1L);
        waitingQueueRepository.enter(2L);

        // act — 1L 이 새로고침하듯 다시 진입 시도
        waitingQueueRepository.enter(1L);

        // assert — 1L 은 여전히 0번, 뒤로 밀리지 않는다
        assertThat(waitingQueueRepository.findRank(1L)).hasValue(0L);
        assertThat(waitingQueueRepository.findRank(2L)).hasValue(1L);
    }

    @DisplayName("대기열 전체 인원 수를 조회할 수 있다.")
    @Test
    void size_returnsTotalWaitingCount() {
        // arrange
        waitingQueueRepository.enter(1L);
        waitingQueueRepository.enter(2L);
        waitingQueueRepository.enter(3L);

        // act & assert
        assertThat(waitingQueueRepository.size()).isEqualTo(3L);
    }

    @DisplayName("대기열 앞 N명을 꺼내면 그 유저들이 제거되고 순번 순서대로 반환된다.")
    @Test
    void popMin_removesAndReturnsFrontUsers() {
        // arrange
        waitingQueueRepository.enter(1L);
        waitingQueueRepository.enter(2L);
        waitingQueueRepository.enter(3L);

        // act
        List<Long> popped = waitingQueueRepository.popMin(2);

        // assert
        assertThat(popped).containsExactly(1L, 2L);
        assertThat(waitingQueueRepository.size()).isEqualTo(1L);
        assertThat(waitingQueueRepository.findRank(3L)).hasValue(0L);
    }
}
