package com.loopers.interfaces.api.ranking;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Ranking V1 API", description = "상품 랭킹 API")
public interface RankingV1ApiSpec {

    @Operation(summary = "일간 랭킹 조회",
            description = "date(yyyyMMdd, 기본 오늘) 랭킹판을 점수 내림차순으로 페이징 조회. 상품/브랜드 정보 포함.")
    ApiResponse<RankingV1Dto.RankingPageResponse> getRankings(String date, Integer page, Integer size);
}
