package com.loopers.application.ranking;

import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.ranking.RankedProduct;
import com.loopers.domain.ranking.RankingKey;
import com.loopers.domain.ranking.RankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class RankingFacade {

    private final RankingRepository rankingRepository;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;

    /**
     * 랭킹 페이지 조회. ZSET 이 주는 건 상품 ID + 점수뿐이므로,
     * 상품/브랜드 정보를 IN 쿼리로 한 번에 붙인다(Aggregation). page 는 1-based.
     */
    public List<RankingInfo> getRankingPage(LocalDate date, int page, int size) {
        long offset = (long) (page - 1) * size;
        List<RankedProduct> ranked = rankingRepository.findPage(RankingKey.daily(date), offset, size);
        if (ranked.isEmpty()) {
            return List.of();
        }

        // 상품 일괄 조회 후 ID → 상품 매핑 (IN 쿼리는 순서를 안 지키므로 랭킹 순서로 재조립)
        List<Long> productIds = ranked.stream().map(RankedProduct::productId).toList();
        Map<Long, ProductModel> productMap = productRepository.findAllByIds(productIds).stream()
                .collect(Collectors.toMap(ProductModel::getId, p -> p));

        Set<Long> brandIds = productMap.values().stream()
                .map(ProductModel::getBrandId)
                .collect(Collectors.toSet());
        Map<Long, BrandModel> brandMap = new HashMap<>();
        for (Long id : brandIds) {
            brandRepository.findById(id).ifPresent(b -> brandMap.put(id, b));
        }

        List<RankingInfo> result = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            RankedProduct entry = ranked.get(i);
            ProductModel product = productMap.get(entry.productId());
            if (product == null) {
                // 랭킹판엔 있지만 DB 엔 없는 상품(삭제 등) — 항목만 건너뛰고 순위 번호는 유지
                log.warn("[ranking] 랭킹판의 상품이 DB 에 없음 productId={}", entry.productId());
                continue;
            }
            long rank = offset + i + 1;   // 1-based 순위
            result.add(RankingInfo.of(rank, entry.score(), product, brandMap.get(product.getBrandId())));
        }
        return result;
    }

    /**
     * 오늘 랭킹판에서의 순위 (1-based). 랭킹에 없으면 null.
     * 순위는 상품 상세의 부가 정보일 뿐이므로, Redis 장애가 상세 조회를 죽이지 않도록 여기서 격리한다.
     */
    public Long getTodayRank(Long productId) {
        try {
            return rankingRepository.findRank(RankingKey.today(), productId)
                    .map(zeroBased -> zeroBased + 1)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("[ranking] 순위 조회 실패 — 상세 응답엔 rank=null 로 계속 진행 productId={}", productId, e);
            return null;
        }
    }
}
