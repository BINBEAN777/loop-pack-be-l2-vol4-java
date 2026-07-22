package com.loopers.domain.ranking;

/**
 * 이벤트 → 랭킹 점수 가중치 정책.
 *
 * <p>지표별 스케일이 달라(조회 >> 좋아요 >> 주문) 단순 합산하면 조회 수가 랭킹을 지배한다.
 * 구매 결정에 가까운 신호일수록 높게: view 0.1 < like 0.2 < order 0.7 (합계 1).
 * 주문은 수량에 비례한다. (단가 반영은 이벤트 페이로드에 가격이 없어 보류 — 확장 지점)
 *
 * <p>가중치를 바꿔도 이미 ZSET 에 합산된 과거 점수는 소급되지 않는다.
 * 실시간 가중치 조절이 필요해지면 지표별 ZSET 분리 + ZUNIONSTORE 재합산으로 확장한다.
 */
public final class RankingScorePolicy {

    private static final double VIEW_WEIGHT = 0.1;
    private static final double LIKE_WEIGHT = 0.2;
    private static final double ORDER_WEIGHT = 0.7;

    private RankingScorePolicy() {
    }

    public static double view() {
        return VIEW_WEIGHT;
    }

    public static double likeAdded() {
        return LIKE_WEIGHT;
    }

    /** 좋아요 취소는 더했던 만큼 되돌린다 — 취소가 랭킹에 남아있으면 어뷰징(좋아요→취소 반복)에 취약. */
    public static double likeRemoved() {
        return -LIKE_WEIGHT;
    }

    public static double order(int quantity) {
        return ORDER_WEIGHT * quantity;
    }
}
