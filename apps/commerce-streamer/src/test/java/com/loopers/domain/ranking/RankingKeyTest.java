package com.loopers.domain.ranking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RankingKeyTest {

    @DisplayName("날짜로 일간 랭킹 키를 만든다. (ranking:all:yyyyMMdd)")
    @Test
    void createsDailyKey_fromDate() {
        RankingKey key = RankingKey.daily(LocalDate.of(2026, 7, 16));

        assertThat(key.value()).isEqualTo("ranking:all:20260716");
    }

    @DisplayName("이벤트 발생 시각(epoch millis)을 Asia/Seoul 기준 날짜로 변환해 키를 만든다.")
    @Test
    void createsKey_fromEpochMillis_inSeoulZone() {
        // arrange — 서울 2026-07-16 10:30
        long occurredAt = ZonedDateTime.of(2026, 7, 16, 10, 30, 0, 0, RankingKey.ZONE)
                .toInstant().toEpochMilli();

        // act
        RankingKey key = RankingKey.fromEpochMillis(occurredAt);

        // assert
        assertThat(key.value()).isEqualTo("ranking:all:20260716");
    }

    @DisplayName("UTC 로는 어제라도 서울 기준으로 오늘이면 오늘 키가 된다. (자정 경계)")
    @Test
    void usesSeoulDate_notUtcDate_atMidnightBoundary() {
        // arrange — UTC 2026-07-16 16:00 = 서울 2026-07-17 01:00
        long occurredAt = ZonedDateTime.of(2026, 7, 16, 16, 0, 0, 0, java.time.ZoneOffset.UTC)
                .toInstant().toEpochMilli();

        // act
        RankingKey key = RankingKey.fromEpochMillis(occurredAt);

        // assert — UTC 날짜(0716)가 아니라 서울 날짜(0717)
        assertThat(key.value()).isEqualTo("ranking:all:20260717");
    }

    @DisplayName("같은 날짜면 같은 키다. (VO 동등성)")
    @Test
    void sameDate_isEqualKey() {
        assertThat(RankingKey.daily(LocalDate.of(2026, 7, 16)))
                .isEqualTo(RankingKey.daily(LocalDate.of(2026, 7, 16)));
    }
}
