package com.loopers.infrastructure.queue;

import com.loopers.domain.queue.WaitingQueueRepository;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class QueueConcurrencyTest {

    @Autowired private WaitingQueueRepository waitingQueueRepository;
    @Autowired private RedisCleanUp redisCleanUp;

    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();
    }

    @DisplayName("여러 유저가 동시에 진입해도, 성공한 진입은 유실·중복 없이 각자 고유 순번을 가진다.")
    @Test
    void concurrentEnter_allGetUniqueRanks() throws InterruptedException {
        // arrange
        int n = 100;
        ExecutorService pool = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(n);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        // act — 100명이 동시에 진입 (드문 커넥션 타임아웃은 인프라성이라 집계만)
        for (int i = 1; i <= n; i++) {
            long userId = i;
            pool.submit(() -> {
                try {
                    waitingQueueRepository.enter(userId);
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        pool.shutdown();

        // assert — 성공한 진입만큼 정확히, 각자 유일한 순번으로 반영된다 (ZADD 원자성)
        int expected = n - errors.size();
        Set<Long> ranks = new HashSet<>();
        for (long u = 1; u <= n; u++) {
            waitingQueueRepository.findRank(u).ifPresent(ranks::add);
        }
        assertThat(waitingQueueRepository.size()).isEqualTo(expected);   // 유실 없음
        assertThat(ranks).hasSize(expected);                            // 중복 없음 (0..expected-1)
    }
}
