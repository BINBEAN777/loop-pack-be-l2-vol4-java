package com.loopers.application.outbox;

import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import com.loopers.domain.outbox.OutboxStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock private OutboxEventRepository outboxRepository;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxEvent pendingEvent() {
        return OutboxEvent.create("evt-1", "product", "1", "LIKE_ADDED",
                "catalog-events", "1", "{\"eventType\":\"LIKE_ADDED\"}");
    }

    @DisplayName("발행에 성공하면 outbox 는 SENT 로 마킹된다.")
    @Test
    void marksSent_onSuccess() {
        OutboxEvent event = pendingEvent();
        when(outboxRepository.findPending(anyInt())).thenReturn(List.of(event));
        doReturn(CompletableFuture.completedFuture(null))
                .when(kafkaTemplate).send(anyString(), anyString(), anyString());

        new OutboxRelay(outboxRepository, kafkaTemplate, 200).relay();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.SENT);
    }

    @DisplayName("발행에 실패하면 PENDING 을 유지해 다음 주기에 재시도한다. (At-Least-Once)")
    @Test
    void keepsPending_onFailure() {
        OutboxEvent event = pendingEvent();
        when(outboxRepository.findPending(anyInt())).thenReturn(List.of(event));
        doReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")))
                .when(kafkaTemplate).send(anyString(), anyString(), anyString());

        new OutboxRelay(outboxRepository, kafkaTemplate, 200).relay();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }
}
