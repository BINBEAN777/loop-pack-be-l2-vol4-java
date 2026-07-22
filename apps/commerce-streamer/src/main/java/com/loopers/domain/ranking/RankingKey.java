package com.loopers.domain.ranking;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 일간 랭킹판을 가리키는 키 (VO). 같은 날짜면 같은 키다.
 *
 * <p>날짜는 <b>이벤트 발생 시각(occurredAt)</b> 기준으로 계산한다 — 컨슈머가 밀려서
 * 어제 이벤트를 오늘 소비해도 "발생한 날"의 랭킹판에 반영되도록.
 * epoch millis → 날짜 변환은 타임존 없이는 정의되지 않으므로 서비스 기준(Asia/Seoul)으로 고정한다.
 *
 * <p>키 포맷(ranking:all:yyyyMMdd)은 commerce-api(조회 측)와 공유하는 계약이다.
 * Kafka 메시지 계약처럼 양쪽에 각자 두고, 형식 변경 시 함께 바꾼다.
 */
public record RankingKey(LocalDate date) {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    public static RankingKey daily(LocalDate date) {
        return new RankingKey(date);
    }

    public static RankingKey fromEpochMillis(long epochMillis) {
        return new RankingKey(Instant.ofEpochMilli(epochMillis).atZone(ZONE).toLocalDate());
    }

    public String value() {
        return "ranking:all:" + date.format(DATE_FORMAT);
    }
}
