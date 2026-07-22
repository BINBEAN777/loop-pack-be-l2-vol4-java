package com.loopers.domain.ranking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RankingScorePolicyTest {

    @DisplayName("주문 1건(수량 1)은 좋아요 3건보다 점수가 높다. (가중치 설계의 핵심 제약)")
    @Test
    void oneOrder_beatsThreeLikes() {
        double oneOrder = RankingScorePolicy.order(1);
        double threeLikes = RankingScorePolicy.likeAdded() * 3;

        assertThat(oneOrder).isGreaterThan(threeLikes);
    }

    @DisplayName("좋아요 1건은 조회 1건보다 점수가 높다.")
    @Test
    void oneLike_beatsOneView() {
        assertThat(RankingScorePolicy.likeAdded()).isGreaterThan(RankingScorePolicy.view());
    }

    @DisplayName("주문 점수는 수량에 비례한다.")
    @Test
    void orderScore_isProportionalToQuantity() {
        assertThat(RankingScorePolicy.order(3))
                .isCloseTo(RankingScorePolicy.order(1) * 3, within(1e-9));
    }

    @DisplayName("좋아요 취소는 좋아요와 정확히 대칭이다. (반복 어뷰징 시 합이 0)")
    @Test
    void likeRemoved_cancelsLikeAdded() {
        double sum = RankingScorePolicy.likeAdded() + RankingScorePolicy.likeRemoved();

        assertThat(sum).isCloseTo(0.0, within(1e-9));
    }
}
