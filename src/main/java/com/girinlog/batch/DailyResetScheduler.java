package com.girinlog.batch;

import com.girinlog.common.time.ServiceDay;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * 매일 06:00 KST에 전날 serviceDate의 작업 공간을 정리한다(시나리오 4).
 * 06:00 경계가 막 지난 시점이므로 "전날"은 오늘 serviceDate - 1일이다.
 */
@Component
public class DailyResetScheduler {

    private final DailyResetBatchService dailyResetBatchService;
    private final Clock clock;

    public DailyResetScheduler(DailyResetBatchService dailyResetBatchService, Clock clock) {
        this.dailyResetBatchService = dailyResetBatchService;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Seoul")
    public void runDailyReset() {
        LocalDate yesterday = ServiceDay.today(clock).minusDays(1);
        dailyResetBatchService.runDailyReset(yesterday);
    }
}
