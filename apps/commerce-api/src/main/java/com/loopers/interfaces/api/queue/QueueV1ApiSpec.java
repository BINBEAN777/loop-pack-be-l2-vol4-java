package com.loopers.interfaces.api.queue;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Queue V1 API", description = "주문 대기열 관련 API")
public interface QueueV1ApiSpec {

    @Operation(summary = "대기열 진입", description = "대기열에 진입하고 현재 순번과 예상 대기시간을 반환한다.")
    ApiResponse<QueueV1Dto.EnterResponse> enter(Long userId);

    @Operation(summary = "순번 조회", description = "현재 순번과 예상 대기시간을 반환한다. 대기열에 없으면 404.")
    ApiResponse<QueueV1Dto.PositionResponse> position(Long userId);
}
