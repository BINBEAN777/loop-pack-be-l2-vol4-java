package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.metrics.MetricsEventHandler;
import com.loopers.confg.kafka.KafkaConfig;
import com.loopers.interfaces.consumer.message.CatalogEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * catalog-events 소비 → product_metrics(좋아요/조회) 집계.
 * Consumer Group 을 order 와 분리해 관심사별로 독립 처리한다. manual ack: 배치 처리 후 오프셋 커밋.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class CatalogEventConsumer {

    private final MetricsEventHandler metricsEventHandler;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "catalog-events",
            groupId = "catalog-metrics-consumer",
            containerFactory = KafkaConfig.BATCH_LISTENER,
            autoStartup = "${loopers.consumer.auto-startup:true}"
    )
    public void consume(List<ConsumerRecord<String, byte[]>> records, Acknowledgment acknowledgment) {
        for (ConsumerRecord<String, byte[]> record : records) {
            try {
                CatalogEventMessage message = objectMapper.readValue(record.value(), CatalogEventMessage.class);
                metricsEventHandler.handleCatalog(message);
            } catch (Exception e) {
                // 개별 메시지 처리 실패는 로깅 후 스킵(포이즌 메시지가 파티션을 막지 않게). 필요 시 DLQ.
                log.error("[catalog-events] 처리 실패 offset={} → 스킵", record.offset(), e);
            }
        }
        acknowledgment.acknowledge();
    }
}
