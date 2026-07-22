package com.loopers.application.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.metrics.ProductMetricsRepository;
import com.loopers.interfaces.consumer.message.CatalogEventMessage;
import com.loopers.interfaces.consumer.message.OrderEventMessage;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MetricsEventHandlerTest {

    @Autowired private MetricsEventHandler handler;
    @Autowired private ProductMetricsRepository metricsRepository;
    @Autowired private DatabaseCleanUp databaseCleanUp;

    private static final Long PRODUCT_ID = 100L;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("LIKE_ADDED 를 처리하면 product_metrics 의 like_count 가 1 증가한다.")
    @Test
    void catalog_likeAdded_increasesLikeCount() {
        handler.handleCatalog(new CatalogEventMessage("evt-1", "LIKE_ADDED", PRODUCT_ID, 1L));

        ProductMetrics metrics = metricsRepository.findByProductId(PRODUCT_ID).orElseThrow();
        assertThat(metrics.getLikeCount()).isEqualTo(1);
    }

    @DisplayName("같은 eventId 를 두 번 받아도 한 번만 반영된다. (멱등)")
    @Test
    void catalog_duplicateEventId_appliedOnce() {
        CatalogEventMessage duplicate = new CatalogEventMessage("evt-dup", "LIKE_ADDED", PRODUCT_ID, 1L);

        handler.handleCatalog(duplicate);
        handler.handleCatalog(duplicate);   // 중복 수신 (At-Least-Once)

        ProductMetrics metrics = metricsRepository.findByProductId(PRODUCT_ID).orElseThrow();
        assertThat(metrics.getLikeCount()).isEqualTo(1);   // 2가 아니라 1
    }

    @DisplayName("새로 반영하면 true, 중복이면 false 를 반환한다. (후속 랭킹 반영의 판단 기준)")
    @Test
    void catalog_returnsWhetherNewlyApplied() {
        CatalogEventMessage message = new CatalogEventMessage("evt-ret", "LIKE_ADDED", PRODUCT_ID, 1L);

        boolean first = handler.handleCatalog(message);
        boolean second = handler.handleCatalog(message);

        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }

    @DisplayName("ORDER_PLACED 를 처리하면 라인별 상품의 sales_count 가 수량만큼 증가한다.")
    @Test
    void order_placed_increasesSalesCount() {
        OrderEventMessage message = new OrderEventMessage(
                "evt-order-1", "ORDER_PLACED", 1L, 7L, 1L,
                List.of(new OrderEventMessage.Line(PRODUCT_ID, 3)));

        handler.handleOrder(message);

        ProductMetrics metrics = metricsRepository.findByProductId(PRODUCT_ID).orElseThrow();
        assertThat(metrics.getSalesCount()).isEqualTo(3);
    }
}
