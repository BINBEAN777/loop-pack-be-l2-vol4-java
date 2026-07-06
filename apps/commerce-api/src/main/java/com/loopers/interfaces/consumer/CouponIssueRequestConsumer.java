package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.coupon.CouponIssuanceService;
import com.loopers.application.outbox.message.CouponIssueMessage;
import com.loopers.confg.kafka.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * coupon-issue-requests 소비 → 실제 쿠폰 발급.
 *
 * <p>key=couponId 라서 같은 쿠폰의 요청은 한 파티션에서 순서대로 처리된다. manual ack: 배치 처리 후 커밋.
 * 발급 자체의 동시성 제어(수량 제한)는 {@link CouponIssuanceService} 의 원자적 재고 차감이 담당한다.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class CouponIssueRequestConsumer {

    private final CouponIssuanceService couponIssuanceService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "coupon-issue-requests",
            groupId = "coupon-issuer",
            containerFactory = KafkaConfig.BATCH_LISTENER,
            autoStartup = "${loopers.consumer.auto-startup:true}"
    )
    public void consume(List<ConsumerRecord<String, byte[]>> records, Acknowledgment acknowledgment) {
        for (ConsumerRecord<String, byte[]> record : records) {
            try {
                CouponIssueMessage message = objectMapper.readValue(record.value(), CouponIssueMessage.class);
                couponIssuanceService.issue(message.requestId(), message.couponId(), message.userId());
            } catch (Exception e) {
                log.error("[coupon-issue-requests] 처리 실패 offset={} → 스킵", record.offset(), e);
            }
        }
        acknowledgment.acknowledge();
    }
}
