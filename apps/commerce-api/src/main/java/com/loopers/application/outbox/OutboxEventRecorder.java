package com.loopers.application.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.outbox.message.CatalogEventMessage;
import com.loopers.application.outbox.message.OrderEventMessage;
import com.loopers.domain.like.event.LikeAddedEvent;
import com.loopers.domain.like.event.LikeRemovedEvent;
import com.loopers.domain.order.event.OrderPlacedEvent;
import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import com.loopers.domain.product.event.ProductViewedEvent;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 도메인 이벤트를 Outbox 행으로 기록한다.
 *
 * <p>{@code @EventListener} 는 publishEvent() 시점에 <b>동기(같은 트랜잭션)</b>로 실행된다.
 * 따라서 outbox 저장이 도메인 변경(좋아요/주문)과 하나의 트랜잭션으로 커밋된다 → 이중 쓰기 문제 회피.
 * (실제 Kafka 발행은 커밋 이후 {@link OutboxRelay} 가 담당한다)
 */
@RequiredArgsConstructor
@Component
public class OutboxEventRecorder {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @EventListener
    public void on(LikeAddedEvent event) {
        recordCatalog("LIKE_ADDED", event.productId());
    }

    @EventListener
    public void on(LikeRemovedEvent event) {
        recordCatalog("LIKE_REMOVED", event.productId());
    }

    @EventListener
    public void on(ProductViewedEvent event) {
        recordCatalog("PRODUCT_VIEWED", event.productId());
    }

    @EventListener
    public void on(OrderPlacedEvent event) {
        String eventId = UUID.randomUUID().toString();
        List<OrderEventMessage.Line> lines = event.lines().stream()
                .map(l -> new OrderEventMessage.Line(l.productId(), l.quantity()))
                .toList();
        OrderEventMessage message = new OrderEventMessage(
                eventId, "ORDER_PLACED", event.orderId(), event.userId(), System.currentTimeMillis(), lines);
        outboxRepository.save(OutboxEvent.create(
                eventId, "order", String.valueOf(event.orderId()), "ORDER_PLACED",
                Topics.ORDER_EVENTS, String.valueOf(event.orderId()), serialize(message)));
    }

    private void recordCatalog(String eventType, Long productId) {
        String eventId = UUID.randomUUID().toString();
        CatalogEventMessage message = new CatalogEventMessage(
                eventId, eventType, productId, System.currentTimeMillis());
        outboxRepository.save(OutboxEvent.create(
                eventId, "product", String.valueOf(productId), eventType,
                Topics.CATALOG_EVENTS, String.valueOf(productId), serialize(message)));
    }

    private String serialize(Object message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "이벤트 직렬화 실패: " + e.getMessage());
        }
    }
}
