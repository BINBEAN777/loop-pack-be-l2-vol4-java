package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingInfo;

import java.util.List;

public class RankingV1Dto {

    public record RankingItemResponse(
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
        public static RankingItemResponse from(RankingInfo info) {
            return new RankingItemResponse(
                    info.rank(), info.score(), info.productId(), info.name(), info.price(),
                    info.likeCount(), info.brandId(), info.brandName(), info.imageUrl()
            );
        }
    }

    public record RankingPageResponse(
            String date,
            int page,
            int size,
            List<RankingItemResponse> items
    ) {
        public static RankingPageResponse of(String date, int page, int size, List<RankingInfo> infos) {
            return new RankingPageResponse(
                    date, page, size,
                    infos.stream().map(RankingItemResponse::from).toList()
            );
        }
    }
}
