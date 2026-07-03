package com.loopers.domain.outbox;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Transactional Outbox.
 *
 * <p>도메인 변경과 <b>같은 트랜잭션</b>에서 이 행을 저장한다(이중 쓰기 문제 회피).
 * 별도 릴레이가 PENDING 행을 Kafka 로 발행(At-Least-Once)하고 SENT 로 마킹한다.
 * eventId 는 소비자 멱등 처리(event_handled)의 키가 된다.
 */
@Entity
@Table(name = "outbox_event", indexes = {
        @Index(name = "idx_outbox_status", columnList = "status, id")
})
public class OutboxEvent extends BaseEntity {

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(name = "partition_key", nullable = false, length = 100)
    private String partitionKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    protected OutboxEvent() {}

    private OutboxEvent(String eventId, String aggregateType, String aggregateId,
                        String eventType, String topic, String partitionKey, String payload) {
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.topic = topic;
        this.partitionKey = partitionKey;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
    }

    public static OutboxEvent create(String eventId, String aggregateType, String aggregateId,
                                     String eventType, String topic, String partitionKey, String payload) {
        return new OutboxEvent(eventId, aggregateType, aggregateId, eventType, topic, partitionKey, payload);
    }

    public void markSent() {
        this.status = OutboxStatus.SENT;
    }

    public String getEventId() { return eventId; }
    public String getAggregateType() { return aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getTopic() { return topic; }
    public String getPartitionKey() { return partitionKey; }
    public String getPayload() { return payload; }
    public OutboxStatus getStatus() { return status; }
}
