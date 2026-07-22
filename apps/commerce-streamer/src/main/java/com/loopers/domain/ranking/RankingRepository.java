package com.loopers.domain.ranking;

import java.util.Map;

public interface RankingRepository {

    /**
     * 한 랭킹판(key)에 상품별 점수 증분을 반영한다.
     * 배치로 모아 호출하는 것을 전제로 한다 — 메시지 1건당 1회 호출하지 말 것.
     */
    void incrementScores(RankingKey key, Map<Long, Double> scoreDeltaByProductId);
}
