package com.loopers.application.outbox;

import com.loopers.application.like.LikeFacade;
import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxStatus;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.vo.Money;
import com.loopers.domain.vo.Quantity;
import com.loopers.infrastructure.outbox.OutboxEventJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 좋아요(도메인 변경)와 Outbox 기록이 같은 트랜잭션에서 이루어지는지 검증.
 * (릴레이는 test 프로파일에서 꺼져 있으므로 PENDING 상태로 남는다)
 */
@SpringBootTest
class OutboxRecordingTest {

    @Autowired private LikeFacade likeFacade;
    @Autowired private BrandRepository brandRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OutboxEventJpaRepository outboxJpaRepository;
    @Autowired private DatabaseCleanUp databaseCleanUp;

    private Long productId;

    @BeforeEach
    void setUp() {
        BrandModel brand = brandRepository.save(new BrandModel("Nike", null));
        productId = productRepository.save(
                new ProductModel(brand.getId(), "운동화", null, Money.of(1000L), Quantity.of(10), null)).getId();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("좋아요를 등록하면 catalog-events 토픽 대상 PENDING outbox 가 상품ID 키로 기록된다.")
    @Test
    void like_writesPendingOutbox() {
        likeFacade.like(1L, productId);

        List<OutboxEvent> events = outboxJpaRepository.findAll();
        assertThat(events).hasSize(1);
        OutboxEvent event = events.get(0);
        assertThat(event.getTopic()).isEqualTo("catalog-events");
        assertThat(event.getEventType()).isEqualTo("LIKE_ADDED");
        assertThat(event.getPartitionKey()).isEqualTo(String.valueOf(productId));
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getPayload()).contains("LIKE_ADDED");
    }
}
