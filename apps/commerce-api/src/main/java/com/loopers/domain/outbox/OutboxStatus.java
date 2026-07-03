package com.loopers.domain.outbox;

public enum OutboxStatus {
    PENDING,   // 아직 Kafka 로 발행되지 않음
    SENT       // 발행 완료
}
