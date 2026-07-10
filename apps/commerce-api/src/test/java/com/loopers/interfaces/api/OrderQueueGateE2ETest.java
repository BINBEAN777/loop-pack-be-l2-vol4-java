package com.loopers.interfaces.api;

import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.queue.EntryTokenStore;
import com.loopers.domain.vo.Money;
import com.loopers.domain.vo.Quantity;
import com.loopers.interfaces.api.order.OrderV1Dto;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "loopers.queue.gate.enabled=true")
class OrderQueueGateE2ETest {

    private static final String ENDPOINT = "/api/v1/orders";

    @Autowired private TestRestTemplate testRestTemplate;
    @Autowired private DatabaseCleanUp databaseCleanUp;
    @Autowired private RedisCleanUp redisCleanUp;
    @Autowired private BrandRepository brandRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private EntryTokenStore entryTokenStore;

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
        redisCleanUp.truncateAll();
    }

    private HttpEntity<OrderV1Dto.OrderRequest> request(OrderV1Dto.OrderRequest body, Long userId, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Loopers-UserId", String.valueOf(userId));
        if (token != null) {
            headers.add("X-Entry-Token", token);
        }
        return new HttpEntity<>(body, headers);
    }

    @DisplayName("관문 ON — 유효한 토큰으로 주문하면 200 이고, 사용된 토큰은 삭제된다.")
    @Test
    void order_withValidToken_succeeds_andConsumesToken() {
        Long userId = 1L;
        String token = entryTokenStore.issue(userId);
        OrderV1Dto.OrderRequest body = new OrderV1Dto.OrderRequest(
                List.of(new OrderV1Dto.OrderItemRequest(productId, 1)));

        ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>> type = new ParameterizedTypeReference<>() {};
        ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response =
                testRestTemplate.exchange(ENDPOINT, HttpMethod.POST, request(body, userId, token), type);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entryTokenStore.find(userId)).isEmpty();   // 성공 후 소진됨
    }

    @DisplayName("관문 ON — 토큰 없이 주문하면 403 이다.")
    @Test
    void order_withoutToken_isForbidden() {
        OrderV1Dto.OrderRequest body = new OrderV1Dto.OrderRequest(
                List.of(new OrderV1Dto.OrderItemRequest(productId, 1)));

        ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>> type = new ParameterizedTypeReference<>() {};
        ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response =
                testRestTemplate.exchange(ENDPOINT, HttpMethod.POST, request(body, 1L, null), type);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
