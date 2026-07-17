package com.loopers.interfaces.api;

import com.loopers.config.redis.RedisConfig;
import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.ranking.RankingKey;
import com.loopers.domain.vo.Money;
import com.loopers.domain.vo.Quantity;
import com.loopers.interfaces.api.product.ProductV1Dto;
import com.loopers.interfaces.api.ranking.RankingV1Dto;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RankingV1ApiE2ETest {

    private static final String ENDPOINT = "/api/v1/rankings";
    private static final String ENDPOINT_PRODUCT_DETAIL = "/api/v1/products/{productId}";

    @Autowired private TestRestTemplate testRestTemplate;
    @Autowired private DatabaseCleanUp databaseCleanUp;
    @Autowired private RedisCleanUp redisCleanUp;
    @Autowired private BrandRepository brandRepository;
    @Autowired private ProductRepository productRepository;

    @Autowired
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private RedisTemplate<String, String> redisTemplate;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.truncateAll();
    }

    private ProductModel seedProduct(BrandModel brand, String name, long price) {
        return productRepository.save(new ProductModel(
                brand.getId(), name, name + " 설명", Money.of(price), Quantity.of(10), "img.png"));
    }

    /** streamer 가 적재했을 상태를 재현 — 오늘 랭킹판에 점수 세팅 */
    private void seedScore(LocalDate date, Long productId, double score) {
        redisTemplate.opsForZSet().add(RankingKey.daily(date).value(), String.valueOf(productId), score);
    }

    @DisplayName("GET /api/v1/rankings")
    @Nested
    class GetRankings {

        @DisplayName("점수 내림차순으로 상품정보가 Aggregation 되어 반환된다.")
        @Test
        void returnsRankings_withProductInfo_orderedByScoreDesc() {
            // arrange — 점수: 신발(10.0) > 가방(5.0) > 모자(1.0)
            BrandModel brand = brandRepository.save(new BrandModel("Nike", "스포츠"));
            ProductModel shoes = seedProduct(brand, "신발", 50_000L);
            ProductModel bag = seedProduct(brand, "가방", 30_000L);
            ProductModel cap = seedProduct(brand, "모자", 10_000L);
            LocalDate today = LocalDate.now(RankingKey.ZONE);
            seedScore(today, shoes.getId(), 10.0);
            seedScore(today, bag.getId(), 5.0);
            seedScore(today, cap.getId(), 1.0);

            // act
            ParameterizedTypeReference<ApiResponse<RankingV1Dto.RankingPageResponse>> type =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<RankingV1Dto.RankingPageResponse>> response =
                    testRestTemplate.exchange(ENDPOINT, HttpMethod.GET, null, type);

            // assert — 순서, 1-based 순위, 상품/브랜드 정보 동봉
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            List<RankingV1Dto.RankingItemResponse> items = response.getBody().data().items();
            assertThat(items).hasSize(3);
            assertThat(items.get(0).productId()).isEqualTo(shoes.getId());
            assertThat(items.get(0).rank()).isEqualTo(1);
            assertThat(items.get(0).name()).isEqualTo("신발");
            assertThat(items.get(0).brandName()).isEqualTo("Nike");
            assertThat(items.get(0).price()).isEqualTo(50_000L);
            assertThat(items.get(1).productId()).isEqualTo(bag.getId());
            assertThat(items.get(2).productId()).isEqualTo(cap.getId());
        }

        @DisplayName("페이징: 2번째 페이지의 순위는 이어서 매겨진다.")
        @Test
        void paging_continuesRankNumbers() {
            // arrange — 3개 상품, size=2 로 2페이지 조회하면 3위 하나
            BrandModel brand = brandRepository.save(new BrandModel("Nike", "스포츠"));
            ProductModel first = seedProduct(brand, "1등", 1000L);
            ProductModel second = seedProduct(brand, "2등", 1000L);
            ProductModel third = seedProduct(brand, "3등", 1000L);
            LocalDate today = LocalDate.now(RankingKey.ZONE);
            seedScore(today, first.getId(), 30.0);
            seedScore(today, second.getId(), 20.0);
            seedScore(today, third.getId(), 10.0);

            // act
            ParameterizedTypeReference<ApiResponse<RankingV1Dto.RankingPageResponse>> type =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<RankingV1Dto.RankingPageResponse>> response =
                    testRestTemplate.exchange(ENDPOINT + "?page=2&size=2", HttpMethod.GET, null, type);

            // assert
            List<RankingV1Dto.RankingItemResponse> items = response.getBody().data().items();
            assertThat(items).hasSize(1);
            assertThat(items.get(0).productId()).isEqualTo(third.getId());
            assertThat(items.get(0).rank()).isEqualTo(3);
        }

        @DisplayName("date 파라미터로 이전 날짜의 랭킹판을 조회할 수 있다.")
        @Test
        void returnsPreviousDateRankings_byDateParam() {
            // arrange — 어제 판에만 점수 존재
            BrandModel brand = brandRepository.save(new BrandModel("Nike", "스포츠"));
            ProductModel shoes = seedProduct(brand, "신발", 50_000L);
            LocalDate yesterday = LocalDate.now(RankingKey.ZONE).minusDays(1);
            seedScore(yesterday, shoes.getId(), 7.0);

            // act
            String url = ENDPOINT + "?date=" + yesterday.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
            ParameterizedTypeReference<ApiResponse<RankingV1Dto.RankingPageResponse>> type =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<RankingV1Dto.RankingPageResponse>> response =
                    testRestTemplate.exchange(url, HttpMethod.GET, null, type);

            // assert — 어제 판 조회는 성공, 오늘 판(빈 판) 조회는 빈 목록
            assertThat(response.getBody().data().items()).hasSize(1);
            ResponseEntity<ApiResponse<RankingV1Dto.RankingPageResponse>> todayResponse =
                    testRestTemplate.exchange(ENDPOINT, HttpMethod.GET, null, type);
            assertThat(todayResponse.getBody().data().items()).isEmpty();
        }

        @DisplayName("랭킹판이 없는 날짜는 빈 목록이 반환된다.")
        @Test
        void returnsEmptyItems_whenNoRankingExists() {
            ParameterizedTypeReference<ApiResponse<RankingV1Dto.RankingPageResponse>> type =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<RankingV1Dto.RankingPageResponse>> response =
                    testRestTemplate.exchange(ENDPOINT + "?date=19700101", HttpMethod.GET, null, type);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().data().items()).isEmpty();
        }

        @DisplayName("잘못된 date 형식은 400 이 반환된다.")
        @Test
        void returnsBadRequest_whenDateFormatIsInvalid() {
            ParameterizedTypeReference<ApiResponse<Object>> type = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response =
                    testRestTemplate.exchange(ENDPOINT + "?date=2026-07-16", HttpMethod.GET, null, type);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @DisplayName("랭킹판에는 있지만 DB 에 없는 상품은 항목에서 제외된다.")
        @Test
        void skipsProducts_missingInDatabase() {
            // arrange — 999999 는 DB 에 없음
            BrandModel brand = brandRepository.save(new BrandModel("Nike", "스포츠"));
            ProductModel shoes = seedProduct(brand, "신발", 50_000L);
            LocalDate today = LocalDate.now(RankingKey.ZONE);
            seedScore(today, 999_999L, 100.0);
            seedScore(today, shoes.getId(), 10.0);

            // act
            ParameterizedTypeReference<ApiResponse<RankingV1Dto.RankingPageResponse>> type =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<RankingV1Dto.RankingPageResponse>> response =
                    testRestTemplate.exchange(ENDPOINT, HttpMethod.GET, null, type);

            // assert — 유령 상품(1위)은 빠지고, 실제 상품은 ZSET 기준 순위(2위)를 유지
            List<RankingV1Dto.RankingItemResponse> items = response.getBody().data().items();
            assertThat(items).hasSize(1);
            assertThat(items.get(0).productId()).isEqualTo(shoes.getId());
            assertThat(items.get(0).rank()).isEqualTo(2);
        }
    }

    @DisplayName("GET /api/v1/products/{productId} — 순위 동봉")
    @Nested
    class GetProductDetailWithRank {

        @DisplayName("랭킹에 있는 상품의 상세를 조회하면 순위(1-based)가 함께 반환된다.")
        @Test
        void returnsRank_whenProductIsRanked() {
            // arrange — 신발이 2위
            BrandModel brand = brandRepository.save(new BrandModel("Nike", "스포츠"));
            ProductModel shoes = seedProduct(brand, "신발", 50_000L);
            LocalDate today = LocalDate.now(RankingKey.ZONE);
            seedScore(today, 999_999L, 100.0);
            seedScore(today, shoes.getId(), 10.0);

            // act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductDetailResponse>> type =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductV1Dto.ProductDetailResponse>> response =
                    testRestTemplate.exchange(ENDPOINT_PRODUCT_DETAIL, HttpMethod.GET, null, type, shoes.getId());

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().data().rank()).isEqualTo(2L);
        }

        @DisplayName("랭킹에 없는 상품의 상세를 조회하면 rank 는 null 이다.")
        @Test
        void returnsNullRank_whenProductIsNotRanked() {
            // arrange — 랭킹판에 아무 점수도 없음
            BrandModel brand = brandRepository.save(new BrandModel("Nike", "스포츠"));
            ProductModel shoes = seedProduct(brand, "신발", 50_000L);

            // act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductDetailResponse>> type =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductV1Dto.ProductDetailResponse>> response =
                    testRestTemplate.exchange(ENDPOINT_PRODUCT_DETAIL, HttpMethod.GET, null, type, shoes.getId());

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().data().rank()).isNull();
        }
    }
}
