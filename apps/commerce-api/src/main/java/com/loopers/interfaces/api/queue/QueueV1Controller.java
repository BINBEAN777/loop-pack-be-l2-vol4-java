package com.loopers.interfaces.api.queue;

import com.loopers.application.queue.QueueFacade;
import com.loopers.application.queue.QueueInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/queue")
public class QueueV1Controller implements QueueV1ApiSpec {

    private final QueueFacade queueFacade;

    @PostMapping("/enter")
    @Override
    public ApiResponse<QueueV1Dto.EnterResponse> enter(
            @RequestHeader(value = "X-Loopers-UserId", required = false) Long userId
    ) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "X-Loopers-UserId 헤더가 필요합니다.");
        }
        QueueInfo info = queueFacade.enter(userId);
        return ApiResponse.success(QueueV1Dto.EnterResponse.from(info));
    }

    @GetMapping("/position")
    @Override
    public ApiResponse<QueueV1Dto.PositionResponse> position(
            @RequestHeader(value = "X-Loopers-UserId", required = false) Long userId
    ) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "X-Loopers-UserId 헤더가 필요합니다.");
        }
        QueueInfo info = queueFacade.position(userId);
        return ApiResponse.success(QueueV1Dto.PositionResponse.from(info));
    }
}
