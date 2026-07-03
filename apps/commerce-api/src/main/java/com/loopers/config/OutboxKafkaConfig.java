package com.loopers.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Outbox 릴레이 전용 Kafka 프로듀서.
 *
 * <p>Outbox 에 저장해 둔 payload(JSON 문자열)를 <b>그대로</b> 토픽에 실어야 하므로 StringSerializer 를 쓴다.
 * <p>{@code acks=all} : 리더 + 모든 ISR 이 기록해야 성공으로 간주(내구성).
 * <p>{@code enable.idempotence=true} : 프로듀서 재시도로 인한 브로커측 중복/재정렬을 방지.
 * (idempotence 는 acks=all, retries&gt;0, max.in.flight&le;5 를 요구 — kafka.yml 에 retries=3 설정됨)
 */
@Configuration
public class OutboxKafkaConfig {

    @Bean
    public ProducerFactory<String, String> outboxProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildProducerProperties());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, String> outboxKafkaTemplate(ProducerFactory<String, String> outboxProducerFactory) {
        return new KafkaTemplate<>(outboxProducerFactory);
    }
}
