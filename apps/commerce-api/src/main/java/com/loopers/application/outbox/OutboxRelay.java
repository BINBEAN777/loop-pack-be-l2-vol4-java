package com.loopers.application.outbox;

import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Outbox 릴레이 — PENDING outbox 행을 주기적으로 Kafka 로 발행한다(At-Least-Once).
 *
 * <p>발행 성공 시 SENT 로 마킹한다. 발행 실패(브로커 장애 등) 시 PENDING 을 유지해 다음 주기에 재시도한다.
 * 발행이 중간에 실패하면 이후 메시지도 중단해 <b>같은 파티션 키의 순서</b>가 어긋나지 않게 한다.
 * (크래시로 발행 후 마킹 전 죽으면 재발행되지만, 소비자의 event_handled 멱등 처리가 중복을 흡수한다)
 *
 * <p>test 프로파일에서는 {@code loopers.outbox.relay.enabled=false} 로 비활성화한다(브로커 미연결).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "loopers.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelay {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> outboxKafkaTemplate;
    private final int batchSize;

    public OutboxRelay(OutboxEventRepository outboxRepository,
                       @Qualifier("outboxKafkaTemplate") KafkaTemplate<String, String> outboxKafkaTemplate,
                       @Value("${loopers.outbox.relay.batch-size:200}") int batchSize) {
        this.outboxRepository = outboxRepository;
        this.outboxKafkaTemplate = outboxKafkaTemplate;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${loopers.outbox.relay.interval-ms:1000}")
    @Transactional
    public void relay() {
        List<OutboxEvent> pending = outboxRepository.findPending(batchSize);
        for (OutboxEvent event : pending) {
            try {
                // key=partitionKey 로 파티션 라우팅 → 같은 상품/주문의 순서 보장.
                // .get() 으로 브로커 ack(acks=all) 를 기다려 발행 성공을 확인한 뒤 SENT 로 마킹.
                outboxKafkaTemplate.send(event.getTopic(), event.getPartitionKey(), event.getPayload()).get();
                event.markSent();   // dirty checking → 커밋 시 UPDATE
            } catch (Exception e) {
                log.warn("[Outbox] 발행 실패 eventId={} topic={} → 다음 주기 재시도",
                        event.getEventId(), event.getTopic(), e);
                break;   // 순서 보장을 위해 이후 발행도 중단
            }
        }
    }
}
