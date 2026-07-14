package com.loopers.interfaces.api;

import com.loopers.domain.queue.EntryTokenStore;
import com.loopers.interfaces.api.queue.QueueV1Dto;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class QueueV1ApiE2ETest {

    private static final String ENTER_ENDPOINT = "/api/v1/queue/enter";
    private static final String POSITION_ENDPOINT = "/api/v1/queue/position";

    @Autowired private TestRestTemplate testRestTemplate;
    @Autowired private RedisCleanUp redisCleanUp;
    @Autowired private EntryTokenStore entryTokenStore;

    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();
    }

    private HttpEntity<Void> requestWithUser(Long userId) {
        HttpHeaders headers = new HttpHeaders();
        if (userId != null) {
            headers.add("X-Loopers-UserId", String.valueOf(userId));
        }
        return new HttpEntity<>(headers);
    }

    private void enter(Long userId) {
        testRestTemplate.exchange(ENTER_ENDPOINT, HttpMethod.POST, requestWithUser(userId),
                new ParameterizedTypeReference<ApiResponse<QueueV1Dto.EnterResponse>>() {});
    }

    @DisplayName("대기열에 처음 진입하면 200 과 순번 0, 예상 대기시간 0 이 응답된다.")
    @Test
    void enter_firstUser_returnsPositionZero() {
        // arrange
        ParameterizedTypeReference<ApiResponse<QueueV1Dto.EnterResponse>> type =
                new ParameterizedTypeReference<>() {};

        // act
        ResponseEntity<ApiResponse<QueueV1Dto.EnterResponse>> response =
                testRestTemplate.exchange(ENTER_ENDPOINT, HttpMethod.POST, requestWithUser(1L), type);

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data().position()).isEqualTo(0L);
        assertThat(response.getBody().data().estimatedWaitSeconds()).isEqualTo(0L);
    }

    @DisplayName("헤더 없이 진입하면 400 이 응답된다.")
    @Test
    void enter_withoutHeader_returnsBadRequest() {
        ParameterizedTypeReference<ApiResponse<QueueV1Dto.EnterResponse>> type =
                new ParameterizedTypeReference<>() {};

        ResponseEntity<ApiResponse<QueueV1Dto.EnterResponse>> response =
                testRestTemplate.exchange(ENTER_ENDPOINT, HttpMethod.POST, requestWithUser(null), type);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @DisplayName("진입 후 순번을 조회하면 자신의 순번이 응답된다.")
    @Test
    void position_afterEnter_returnsOwnPosition() {
        // arrange — 1L 먼저, 2L 나중에 진입
        enter(1L);
        enter(2L);

        // act — 2L 의 순번 조회
        ParameterizedTypeReference<ApiResponse<QueueV1Dto.PositionResponse>> type =
                new ParameterizedTypeReference<>() {};
        ResponseEntity<ApiResponse<QueueV1Dto.PositionResponse>> response =
                testRestTemplate.exchange(POSITION_ENDPOINT, HttpMethod.GET, requestWithUser(2L), type);

        // assert — 2L 은 1번(0-based)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data().position()).isEqualTo(1L);
    }

    @DisplayName("대기열에 없는 유저가 순번을 조회하면 404 가 응답된다.")
    @Test
    void position_notInQueue_returnsNotFound() {
        ParameterizedTypeReference<ApiResponse<QueueV1Dto.PositionResponse>> type =
                new ParameterizedTypeReference<>() {};
        ResponseEntity<ApiResponse<QueueV1Dto.PositionResponse>> response =
                testRestTemplate.exchange(POSITION_ENDPOINT, HttpMethod.GET, requestWithUser(99L), type);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @DisplayName("입장 토큰이 발급된 유저가 순번을 조회하면, 순번 0과 토큰이 함께 응답된다.")
    @Test
    void position_whenAdmitted_returnsPositionZeroWithToken() {
        // arrange — 큐엔 없지만 토큰 보유 (스케줄러가 뽑아 토큰 준 상태 재현)
        String issued = entryTokenStore.issue(1L);

        // act
        ParameterizedTypeReference<ApiResponse<QueueV1Dto.PositionResponse>> type =
                new ParameterizedTypeReference<>() {};
        ResponseEntity<ApiResponse<QueueV1Dto.PositionResponse>> response =
                testRestTemplate.exchange(POSITION_ENDPOINT, HttpMethod.GET, requestWithUser(1L), type);

        // assert — 순번 0 + 토큰 동봉
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data().position()).isEqualTo(0L);
        assertThat(response.getBody().data().token()).isEqualTo(issued);
    }
}
