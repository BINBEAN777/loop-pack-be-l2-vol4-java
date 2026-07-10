package com.loopers.application.queue;

import com.loopers.domain.queue.EntryTokenStore;
import com.loopers.domain.queue.WaitingQueueRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class QueueFacade {

    private final WaitingQueueRepository waitingQueueRepository;
    private final EntryTokenStore entryTokenStore;

    @Value("${loopers.queue.gate.enabled:false}")   // 관문 토글 (기본 꺼짐)
    private boolean gateEnabled;

    /** 대기열에 진입시키고, 현재 순번/예상 대기시간을 돌려준다. */
    public QueueInfo enter(Long userId) {
        waitingQueueRepository.enter(userId);
        long rank = waitingQueueRepository.findRank(userId).orElse(0L);
        return QueueInfo.of(rank);
    }

    /** 현재 순번/예상 대기시간을 조회한다. 대기열에 없으면 404. */
    public QueueInfo position(Long userId) {
        long rank = waitingQueueRepository.findRank(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "대기열에 없습니다."));
        return QueueInfo.of(rank);
    }

    /** 주문 API 진입 관문: 토큰을 검증한다. 관문이 꺼져 있으면 그냥 통과. */
    public void validateEntry(Long userId, String token) {
        if (!gateEnabled) {
            return;   // 관문 OFF → 통과 (평소 / Redis 장애 시 bypass)
        }
        if (token == null || token.isBlank()) {
            throw new CoreException(ErrorType.FORBIDDEN, "입장 토큰이 필요합니다.");
        }
        String stored = entryTokenStore.find(userId)
                .orElseThrow(() -> new CoreException(ErrorType.FORBIDDEN, "유효한 입장 토큰이 없습니다."));
        if (!stored.equals(token)) {
            throw new CoreException(ErrorType.FORBIDDEN, "입장 토큰이 일치하지 않습니다.");
        }
    }

    /** 주문 성공 후 토큰을 소진(삭제)한다. 관문이 꺼져 있으면 아무 것도 하지 않는다. */
    public void consumeEntry(Long userId) {
        if (!gateEnabled) {
            return;
        }
        entryTokenStore.remove(userId);
    }
}
