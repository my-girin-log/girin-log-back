package com.girinlog.common.time;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/**
 * 이 서비스의 "하루"는 자정이 아니라 <b>06:00 KST</b> 에 바뀐다. (conventions/api.md 2절)
 *
 * <p>Diary 자동 정리, "오늘의 메모", 기간 선택이 모두 이 경계를 공유한다.
 * 예: 6/5 04:00 KST 에 남긴 기록은 아직 06:00 전이므로 <b>6/4</b> 의 기록이다.
 *
 * <p>모든 일자 계산은 이 유틸을 통한다. 경계 시각을 코드 곳곳에 흩지 않는다.
 */
public final class ServiceDay {

    /** 일자 경계 시각(KST). 매직 넘버를 두지 않기 위해 한 곳에서 정의한다. */
    public static final LocalTime DAY_BOUNDARY = LocalTime.of(6, 0);

    private ServiceDay() {
    }

    /** 주어진 Clock 기준 현재 서비스 일자(06:00 경계 적용). */
    public static LocalDate today(Clock clock) {
        return serviceDateOf(ZonedDateTime.now(clock));
    }

    /** 임의 시각이 속한 서비스 일자. 06:00 이전이면 전날로 본다. */
    public static LocalDate serviceDateOf(ZonedDateTime when) {
        ZonedDateTime kst = when.withZoneSameInstant(ServiceClockConfig.KST);
        if (kst.toLocalTime().isBefore(DAY_BOUNDARY)) {
            return kst.toLocalDate().minusDays(1);
        }
        return kst.toLocalDate();
    }

    /** 주어진 서비스 일자가 시작되는 절대 시각(해당일 06:00 KST). */
    public static ZonedDateTime startOf(LocalDate serviceDate) {
        return serviceDate.atTime(DAY_BOUNDARY).atZone(ServiceClockConfig.KST);
    }

    /** 다음 06:00 KST 경계까지 남은 시간(스케줄러용). */
    public static Duration untilNextBoundary(Clock clock) {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(ServiceClockConfig.KST);
        ZonedDateTime next = startOf(serviceDateOf(now).plusDays(1));
        return Duration.between(now, next);
    }
}
