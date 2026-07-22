package com.loopers.domain.ranking;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 일간 랭킹판을 가리키는 키 (VO).
 *
 * <p>키 포맷(ranking:all:yyyyMMdd)은 commerce-streamer(적재 측)와 공유하는 계약이다.
 * Kafka 메시지 계약처럼 양쪽에 각자 두고, 형식 변경 시 함께 바꾼다.
 */
public record RankingKey(LocalDate date) {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    public static RankingKey daily(LocalDate date) {
        return new RankingKey(date);
    }

    public static RankingKey today() {
        return new RankingKey(LocalDate.now(ZONE));
    }

    public String value() {
        return "ranking:all:" + date.format(DATE_FORMAT);
    }
}
