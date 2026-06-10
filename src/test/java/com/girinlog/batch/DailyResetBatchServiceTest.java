package com.girinlog.batch;

import com.girinlog.conversation.service.DailyChatSessionService;
import com.girinlog.diary.service.DiaryService;
import com.girinlog.memo.service.MemoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyResetBatchServiceTest {

    private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 6, 9);

    @Mock
    private DailyChatSessionService dailyChatSessionService;
    @Mock
    private DiaryService diaryService;
    @Mock
    private MemoService memoService;

    @InjectMocks
    private DailyResetBatchService batchService;

    @Test
    @DisplayName("자동종료 → 사용자별 Diary 생성 → DRAFT 아카이브 순서로 실행한다")
    void runsStepsInOrder() {
        when(dailyChatSessionService.endOpenSessions(SERVICE_DATE)).thenReturn(2);
        when(dailyChatSessionService.findUserIdsWithSessions(SERVICE_DATE)).thenReturn(List.of(1L, 2L));
        when(diaryService.generateDailyDiary(any(), eq(SERVICE_DATE))).thenReturn(Optional.empty());
        when(memoService.archiveDraftMemos(SERVICE_DATE)).thenReturn(3);

        batchService.runDailyReset(SERVICE_DATE);

        var ordered = inOrder(dailyChatSessionService, diaryService, memoService);
        ordered.verify(dailyChatSessionService).endOpenSessions(SERVICE_DATE);
        ordered.verify(dailyChatSessionService).findUserIdsWithSessions(SERVICE_DATE);
        ordered.verify(diaryService).generateDailyDiary(1L, SERVICE_DATE);
        ordered.verify(diaryService).generateDailyDiary(2L, SERVICE_DATE);
        ordered.verify(memoService).archiveDraftMemos(SERVICE_DATE);
    }

    @Test
    @DisplayName("한 사용자 Diary 생성이 실패해도 나머지 사용자와 아카이브는 계속된다")
    void continuesWhenOneUserFails() {
        when(dailyChatSessionService.endOpenSessions(SERVICE_DATE)).thenReturn(1);
        when(dailyChatSessionService.findUserIdsWithSessions(SERVICE_DATE)).thenReturn(List.of(1L, 2L));
        when(diaryService.generateDailyDiary(1L, SERVICE_DATE)).thenThrow(new RuntimeException("LLM down"));
        when(diaryService.generateDailyDiary(2L, SERVICE_DATE)).thenReturn(Optional.empty());
        when(memoService.archiveDraftMemos(SERVICE_DATE)).thenReturn(0);

        batchService.runDailyReset(SERVICE_DATE);

        verify(diaryService).generateDailyDiary(2L, SERVICE_DATE);
        verify(memoService).archiveDraftMemos(SERVICE_DATE);
    }
}
