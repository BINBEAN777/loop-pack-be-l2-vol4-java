package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.metrics.MetricsEventHandler;
import com.loopers.confg.kafka.KafkaConfig;
import com.loopers.interfaces.consumer.message.OrderEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * order-events 소비 → product_metrics(판매량) 집계.
 * catalog 와 별도 Consumer Group 으로 독립 처리한다. manual ack: 배치 처리 후 오프셋 커밋.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class OrderEventConsumer {

    private final MetricsEventHandler metricsEventHandler;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "order-events",
            groupId = "order-metrics-consumer",
            containerFactory = KafkaConfig.BATCH_LISTENER,
            autoStartup = "${loopers.consumer.auto-startup:true}"
    )
    public void consume(List<ConsumerRecord<String, byte[]>> records, Acknowledgment acknowledgment) {
        for (ConsumerRecord<String, byte[]> record : records) {
            try {
                OrderEventMessage message = objectMapper.readValue(record.value(), OrderEventMessage.class);
                metricsEventHandler.handleOrder(message);
            } catch (Exception e) {
                log.error("[order-events] 처리 실패 offset={} → 스킵", record.offset(), e);
            }
        }
        acknowledgment.acknowledge();
    }
}
