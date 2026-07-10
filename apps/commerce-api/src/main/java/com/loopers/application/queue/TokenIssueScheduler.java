package com.loopers.application.queue;

import com.loopers.domain.queue.EntryTokenStore;
import com.loopers.domain.queue.WaitingQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class TokenIssueScheduler {

    private final WaitingQueueRepository waitingQueueRepository;
    private final EntryTokenStore entryTokenStore;

    @Value("${loopers.queue.scheduler.batch-size:18}")
    private int batchSize;

    /**
     * 100ms 마다 대기열 앞 N명을 꺼내 입장 토큰을 발급한다.
     * (Thundering Herd 완화: 1초에 몰지 않고 100ms 단위로 ~18명씩 분산)
     */
    @Scheduled(fixedDelayString = "${loopers.queue.scheduler.interval-ms:100}")
    public void issueTokens() {
        try {
            List<Long> admitted = waitingQueueRepository.popMin(batchSize);
            for (Long userId : admitted) {
                entryTokenStore.issue(userId);
            }
        } catch (Exception e) {
            // ★ @Scheduled 메서드에서 예외가 밖으로 나가면 이후 실행이 조용히 취소된다.
            //   반드시 여기서 삼켜서 스케줄러가 계속 돌게 한다.
            log.error("토큰 발급 스케줄러 실행 중 오류", e);
        }
    }
}
