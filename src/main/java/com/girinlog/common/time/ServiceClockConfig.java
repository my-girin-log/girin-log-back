package com.girinlog.common.time;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 시각 계산용 {@link Clock} 을 빈으로 제공한다.
 *
 * <p>일자 계산은 반드시 주입된 Clock 을 통한다. 곳곳에서 타임존 없는 {@code LocalDate.now()} 를
 * 쓰지 않는다(conventions/coding.md 7절). 테스트에서는 고정 Clock 으로 교체한다.
 */
@Configuration
public class ServiceClockConfig {

    /** 이 서비스의 기준 타임존. */
    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock serviceClock() {
        return Clock.system(KST);
    }
}
