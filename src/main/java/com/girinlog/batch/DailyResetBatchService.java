package com.girinlog.batch;

import com.girinlog.conversation.service.DailyChatSessionService;
import com.girinlog.diary.service.DiaryService;
import com.girinlog.memo.service.MemoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 06:00 KST 일일 작업 공간 초기화 배치(시나리오 4). 여러 도메인을 공개 서비스로만 조립한다.
 *
 * <p>순서: 전날 OPEN 세션 자동 종료(SYSTEM_ENDED) → 사용자별 Diary 생성(멱등) → DRAFT Memo ARCHIVED.
 * 각 단계/사용자는 개별 트랜잭션이며, 한 사용자의 Diary 생성 실패가 전체 배치를 멈추지 않는다.
 */
@Service
public class DailyResetBatchService {

    private static final Logger log = LoggerFactory.getLogger(DailyResetBatchService.class);

    private final DailyChatSessionService dailyChatSessionService;
    private final DiaryService diaryService;
    private final MemoService memoService;

    public DailyResetBatchService(
            DailyChatSessionService dailyChatSessionService,
            DiaryService diaryService,
            MemoService memoService) {
        this.dailyChatSessionService = dailyChatSessionService;
        this.diaryService = diaryService;
        this.memoService = memoService;
    }

    public void runDailyReset(LocalDate serviceDate) {
        log.info("일일 배치 시작: serviceDate={}", serviceDate);

        int endedSessions = dailyChatSessionService.endOpenSessions(serviceDate);

        List<Long> userIds = dailyChatSessionService.findUserIdsWithSessions(serviceDate);
        int generatedDiaries = 0;
        for (Long userId : userIds) {
            try {
                if (diaryService.generateDailyDiary(userId, serviceDate).isPresent()) {
                    generatedDiaries++;
                }
            } catch (RuntimeException exception) {
                log.warn("Diary 생성 실패 — userId={}, serviceDate={}: {}", userId, serviceDate, exception.getMessage());
            }
        }

        int archivedMemos = memoService.archiveDraftMemos(serviceDate);

        log.info("일일 배치 완료: serviceDate={} 자동종료={} Diary생성={} 아카이브={}",
                serviceDate, endedSessions, generatedDiaries, archivedMemos);
    }
}
