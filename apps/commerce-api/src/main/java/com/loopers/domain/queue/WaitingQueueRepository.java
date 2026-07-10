package com.loopers.domain.queue;

import java.util.List;
import java.util.Optional;

public interface WaitingQueueRepository {

    /** 대기열에 진입한다. 이미 대기 중이면 순번을 유지한다(중복 진입 무시). */
    void enter(Long userId);

    /** 현재 순번(0-based). 대기열에 없으면 empty. */
    Optional<Long> findRank(Long userId);

    /** 현재 대기 중인 전체 인원. */
    long size();

    /** 대기열 맨 앞 N명을 꺼낸다(제거 + 반환). 스케줄러가 입장 처리에 사용. */
    List<Long> popMin(int count);
}
