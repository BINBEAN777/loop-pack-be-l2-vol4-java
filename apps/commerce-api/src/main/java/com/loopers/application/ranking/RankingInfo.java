package com.loopers.application.ranking;

import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.product.ProductModel;

/** 랭킹 한 줄 — 순위 + 점수 + 상품/브랜드 요약 (Aggregation 결과). */
public record RankingInfo(
        long rank,
        double score,
        Long productId,
        String name,
        long price,
        int likeCount,
        Long brandId,
        String brandName,
        String imageUrl
) {
    public static RankingInfo of(long rank, double score, ProductModel product, BrandModel brand) {
        return new RankingInfo(
                rank,
                score,
                product.getId(),
                product.getName(),
                product.getPrice().amount(),
                product.getLikeCount(),
                brand == null ? null : brand.getId(),
                brand == null ? null : brand.getName(),
                product.getImageUrl()
        );
    }
}
