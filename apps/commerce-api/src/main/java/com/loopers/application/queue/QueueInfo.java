package com.loopers.application.queue;

public record QueueInfo(long position, long estimatedWaitSeconds) {

    // Phase 0 처리량 산정 근거: 커넥션풀 50 / 200ms → 250 TPS → 안전마진 70% → 175 TPS
    private static final long THROUGHPUT_PER_SEC = 175;

    /** 순번(rank)으로부터 예상 대기시간을 계산해 조립한다. */
    public static QueueInfo of(long rank) {
        return new QueueInfo(rank, rank / THROUGHPUT_PER_SEC);
    }
}
