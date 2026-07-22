package com.loopers.application.ranking;

import com.loopers.domain.ranking.RankingKey;
import com.loopers.domain.ranking.RankingRepository;
import com.loopers.domain.ranking.RankingScorePolicy;
import com.loopers.interfaces.consumer.message.CatalogEventMessage;
import com.loopers.interfaces.consumer.message.OrderEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 이벤트 배치 → 랭킹 ZSET 점수 반영.
 *
 * <p><b>새로 처리된(applied) 이벤트만</b> 받는 것을 전제로 한다 — 중복 판정(event_handled)은
 * MetricsEventHandler 가 담당하고, 여기서는 그 결과를 신뢰한다.
 *
 * <p>배치 정제: 메시지 1건마다 ZINCRBY 하지 않고, (랭킹판, 상품) 별로 점수를 애플리케이션에서
 * 먼저 합산한 뒤 반영한다. 3000건 배치에 상품이 100종이면 Redis 왕복이 3000 → 최대 100회로 준다.
 *
 * <p>랭킹은 근사치(best-effort)다: Redis 반영 실패는 로깅 후 삼킨다. 여기서 예외를 던지면
 * ack 이 안 되어 배치가 재소비되는데, DB 멱등 처리 때문에 applied 가 비어 랭킹은 어차피 복구되지
 * 않고 metrics 처리만 반복된다. 정확한 원본은 product_metrics 가 갖고 있다.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class RankingEventHandler {

    private final RankingRepository rankingRepository;

    public void handleCatalog(List<CatalogEventMessage> appliedMessages) {
        Map<RankingKey, Map<Long, Double>> deltas = new HashMap<>();
        for (CatalogEventMessage message : appliedMessages) {
            double score = switch (message.eventType()) {
                case "LIKE_ADDED" -> RankingScorePolicy.likeAdded();
                case "LIKE_REMOVED" -> RankingScorePolicy.likeRemoved();
                case "PRODUCT_VIEWED" -> RankingScorePolicy.view();
                default -> 0.0;
            };
            if (score == 0.0) {
                continue;
            }
            accumulate(deltas, RankingKey.fromEpochMillis(message.occurredAt()), message.productId(), score);
        }
        flush(deltas);
    }

    public void handleOrder(List<OrderEventMessage> appliedMessages) {
        Map<RankingKey, Map<Long, Double>> deltas = new HashMap<>();
        for (OrderEventMessage message : appliedMessages) {
            if (!"ORDER_PLACED".equals(message.eventType()) || message.lines() == null) {
                continue;
            }
            RankingKey key = RankingKey.fromEpochMillis(message.occurredAt());
            for (OrderEventMessage.Line line : message.lines()) {
                accumulate(deltas, key, line.productId(), RankingScorePolicy.order(line.quantity()));
            }
        }
        flush(deltas);
    }

    private void accumulate(Map<RankingKey, Map<Long, Double>> deltas, RankingKey key, Long productId, double score) {
        deltas.computeIfAbsent(key, k -> new HashMap<>())
                .merge(productId, score, Double::sum);
    }

    private void flush(Map<RankingKey, Map<Long, Double>> deltas) {
        for (Map.Entry<RankingKey, Map<Long, Double>> entry : deltas.entrySet()) {
            try {
                rankingRepository.incrementScores(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                log.error("[ranking] ZSET 반영 실패 key={} 상품 {}건 — 랭킹은 유실 허용, 스킵",
                        entry.getKey().value(), entry.getValue().size(), e);
            }
        }
    }
}
