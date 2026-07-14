package com.loopers.application.queue;

import java.util.Optional;

public record QueueInfo(long position, long estimatedWaitSeconds, Optional<String> token) {

    // Phase 0 처리량 산정 근거: 커넥션풀 50 / 200ms → 250 TPS → 안전마진 70% → 175 TPS
    private static final long THROUGHPUT_PER_SEC = 175;

    /** 대기 중: 순번으로 예상시간 계산, 토큰 없음. */
    public static QueueInfo of(long rank) {
        return new QueueInfo(rank, rank / THROUGHPUT_PER_SEC, Optional.empty());
    }

    /** 입장 허가(내 차례): 순번 0, 토큰 발급됨. */
    public static QueueInfo admitted(String token) {
        return new QueueInfo(0L, 0L, Optional.of(token));
    }
}
