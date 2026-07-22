package com.loopers.domain.ranking;

import java.util.List;
import java.util.Optional;

public interface RankingRepository {

    /** 점수 내림차순으로 offset 부터 count 개 조회. 랭킹판이 없으면 빈 목록. */
    List<RankedProduct> findPage(RankingKey key, long offset, long count);

    /** 특정 상품의 순위 (0-based). 랭킹판에 없으면 empty. */
    Optional<Long> findRank(RankingKey key, Long productId);

    /** 랭킹판의 전체 상품 수. 랭킹판이 없으면 0. */
    long countMembers(RankingKey key);
}
