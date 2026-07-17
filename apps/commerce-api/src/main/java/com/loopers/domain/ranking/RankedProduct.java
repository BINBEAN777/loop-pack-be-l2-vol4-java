package com.loopers.domain.ranking;

/** 랭킹판의 한 항목 — 상품 ID 와 점수. 순위는 조회 offset 으로부터 계산된다. */
public record RankedProduct(Long productId, double score) {
}
