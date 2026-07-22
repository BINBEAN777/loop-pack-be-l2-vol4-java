package com.loopers.application.metrics;

import com.loopers.domain.handled.HandledEvent;
import com.loopers.domain.handled.HandledEventRepository;
import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.metrics.ProductMetricsRepository;
import com.loopers.interfaces.consumer.message.CatalogEventMessage;
import com.loopers.interfaces.consumer.message.OrderEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이벤트를 받아 product_metrics 를 upsert 하는 집계 처리 — 멱등 보장의 핵심.
 *
 * <p>event_handled(event_id) 로 <b>이미 처리한 이벤트는 건너뛴다</b>(At-Least-Once 로 중복 수신 가능).
 * 지표 반영과 처리 기록을 <b>한 트랜잭션</b>으로 커밋해, "반영은 됐는데 기록은 안 됨" 같은 틈을 없앤다.
 *
 * <p>반환값은 "이번에 새로 반영했는가" — 후속 처리(랭킹 ZSET)가 중복 반영을 피할 때 쓴다.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MetricsEventHandler {

    private final ProductMetricsRepository metricsRepository;
    private final HandledEventRepository handledEventRepository;

    @Transactional
    public boolean handleCatalog(CatalogEventMessage message) {
        if (alreadyHandled(message.eventId())) {
            return false;
        }
        ProductMetrics metrics = getOrCreate(message.productId());
        switch (message.eventType()) {
            case "LIKE_ADDED" -> metrics.addLike(1, message.occurredAt());
            case "LIKE_REMOVED" -> metrics.addLike(-1, message.occurredAt());
            case "PRODUCT_VIEWED" -> metrics.addView(message.occurredAt());
            default -> log.warn("[metrics] 알 수 없는 catalog eventType={}", message.eventType());
        }
        metricsRepository.save(metrics);
        handledEventRepository.save(HandledEvent.of(message.eventId(), message.eventType()));
        return true;
    }

    @Transactional
    public boolean handleOrder(OrderEventMessage message) {
        if (alreadyHandled(message.eventId())) {
            return false;
        }
        if ("ORDER_PLACED".equals(message.eventType()) && message.lines() != null) {
            for (OrderEventMessage.Line line : message.lines()) {
                ProductMetrics metrics = getOrCreate(line.productId());
                metrics.addSales(line.quantity(), message.occurredAt());
                metricsRepository.save(metrics);
            }
        } else {
            log.warn("[metrics] 알 수 없는 order eventType={}", message.eventType());
        }
        handledEventRepository.save(HandledEvent.of(message.eventId(), message.eventType()));
        return true;
    }

    private boolean alreadyHandled(String eventId) {
        return handledEventRepository.existsByEventId(eventId);
    }

    private ProductMetrics getOrCreate(Long productId) {
        return metricsRepository.findByProductId(productId)
                .orElseGet(() -> ProductMetrics.create(productId));
    }
}
