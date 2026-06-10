package com.girinlog.batch;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄링 활성화. 06:00 KST 일일 배치({@link DailyResetScheduler})를 위해 필요하다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
