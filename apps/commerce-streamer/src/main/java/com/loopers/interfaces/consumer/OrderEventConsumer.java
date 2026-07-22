package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.metrics.MetricsEventHandler;
import com.loopers.application.ranking.RankingEventHandler;
import com.loopers.confg.kafka.KafkaConfig;
import com.loopers.interfaces.consumer.message.OrderEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * order-events 소비 → product_metrics(판매량) 집계 + 랭킹 ZSET 반영.
 * catalog 와 별도 Consumer Group 으로 독립 처리한다. manual ack: 배치 처리 후 오프셋 커밋.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class OrderEventConsumer {

    private final MetricsEventHandler metricsEventHandler;
    private final RankingEventHandler rankingEventHandler;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "order-events",
            groupId = "order-metrics-consumer",
            containerFactory = KafkaConfig.BATCH_LISTENER,
            autoStartup = "${loopers.consumer.auto-startup:true}"
    )
    public void consume(List<ConsumerRecord<String, byte[]>> records, Acknowledgment acknowledgment) {
        List<OrderEventMessage> applied = new ArrayList<>();
        for (ConsumerRecord<String, byte[]> record : records) {
            try {
                OrderEventMessage message = objectMapper.readValue(record.value(), OrderEventMessage.class);
                if (metricsEventHandler.handleOrder(message)) {
                    applied.add(message);   // 새로 반영된 이벤트만 랭킹 대상 (중복 제외)
                }
            } catch (Exception e) {
                log.error("[order-events] 처리 실패 offset={} → 스킵", record.offset(), e);
            }
        }
        rankingEventHandler.handleOrder(applied);
        acknowledgment.acknowledge();
    }
}
