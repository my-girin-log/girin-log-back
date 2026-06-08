package com.girinlog.common.time;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceDayTest {

    private static Clock fixedKst(String isoLocalDateTime) {
        ZonedDateTime kst = LocalDate.parse(isoLocalDateTime.substring(0, 10))
                .atTime(java.time.LocalTime.parse(isoLocalDateTime.substring(11)))
                .atZone(ServiceClockConfig.KST);
        return Clock.fixed(kst.toInstant(), ServiceClockConfig.KST);
    }

    @Test
    @DisplayName("06:00 이전(KST)은 전날의 서비스 일자로 본다")
    void beforeBoundaryBelongsToPreviousDay() {
        Clock clock = fixedKst("2026-06-05T04:00");

        assertThat(ServiceDay.today(clock)).isEqualTo(LocalDate.of(2026, 6, 4));
    }

    @Test
    @DisplayName("06:00 정각(KST)부터 당일의 서비스 일자다")
    void atBoundaryBelongsToSameDay() {
        Clock clock = fixedKst("2026-06-05T06:00");

        assertThat(ServiceDay.today(clock)).isEqualTo(LocalDate.of(2026, 6, 5));
    }

    @Test
    @DisplayName("다른 타임존 시각도 KST 06:00 경계로 환산한다")
    void convertsOtherZoneToKstBoundary() {
        // UTC 20:00 == KST 익일 05:00 → 아직 06:00 전이므로 전날
        ZonedDateTime utc = ZonedDateTime.of(2026, 6, 4, 20, 0, 0, 0, ZoneId.of("UTC"));

        assertThat(ServiceDay.serviceDateOf(utc)).isEqualTo(LocalDate.of(2026, 6, 4));
    }

    @Test
    @DisplayName("다음 06:00 경계까지 남은 시간을 계산한다")
    void untilNextBoundaryFromMorning() {
        Clock clock = fixedKst("2026-06-05T07:00");

        // 07:00 → 익일 06:00 = 23시간
        assertThat(ServiceDay.untilNextBoundary(clock).toHours()).isEqualTo(23);
    }

    @Test
    @DisplayName("instant 기반 고정 Clock 도 일관된 일자를 준다")
    void worksWithInstantClock() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-05T00:00:00Z"), ServiceClockConfig.KST);

        // UTC 00:00 == KST 09:00 → 당일
        assertThat(ServiceDay.today(clock)).isEqualTo(LocalDate.of(2026, 6, 5));
    }
}
