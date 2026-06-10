package com.girinlog.conversation.service;

import com.girinlog.conversation.domain.DailyChatSession;
import com.girinlog.conversation.domain.EndedReason;
import com.girinlog.conversation.repository.DailyChatSessionRepository;
import com.girinlog.event.service.EventLogRecorder;
import com.girinlog.memo.repository.MemoSummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * submitAnswer에서 실록이가 종료를 판단(AI_DECIDED)하는 분기 단위 테스트.
 */
class DailyChatSessionAiDecidedTest {

    private static final Long USER_ID = 10L;
    private static final Long SESSION_ID = 1L;

    private final DailyChatSessionRepository sessionRepository = mock(DailyChatSessionRepository.class);
    private final MemoSummaryRepository memoSummaryRepository = mock(MemoSummaryRepository.class);
    private final DailyChatQuestionGenerator questionGenerator = mock(DailyChatQuestionGenerator.class);
    private final EventLogRecorder eventLogRecorder = mock(EventLogRecorder.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-09T05:00:00Z"), ZoneId.of("Asia/Seoul"));

    private DailyChatSessionService service;
    private DailyChatSession session;

    @BeforeEach
    void setUp() {
        service = new DailyChatSessionService(
                sessionRepository, memoSummaryRepository, questionGenerator, eventLogRecorder, clock);
        session = DailyChatSession.start(
                USER_ID,
                LocalDate.of(2026, 6, 9),
                List.of(1L),
                "snapshot",
                "첫 질문",
                OffsetDateTime.now(clock));
        when(sessionRepository.findByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(Optional.of(session));
    }

    @Test
    @DisplayName("실록이가 종료를 판단하면 AI_DECIDED로 세션을 끝낸다")
    void endsWithAiDecidedWhenGeneratorDecides() {
        when(questionGenerator.shouldEnd(session)).thenReturn(true);
        when(questionGenerator.generateClosingMessage(session, EndedReason.AI_DECIDED)).thenReturn("오늘은 충분해.");

        DailyChatSession result = service.submitAnswer(USER_ID, SESSION_ID, "답변");

        assertThat(result.isEnded()).isTrue();
        assertThat(result.endedReason()).isEqualTo(EndedReason.AI_DECIDED);
        verify(questionGenerator, never()).generateFollowUpQuestion(any());
    }

    @Test
    @DisplayName("종료 판단이 아니면 다음 역질문을 이어간다")
    void continuesWhenNotDecidedToEnd() {
        when(questionGenerator.shouldEnd(session)).thenReturn(false);
        when(questionGenerator.generateFollowUpQuestion(session)).thenReturn("다음 질문");

        DailyChatSession result = service.submitAnswer(USER_ID, SESSION_ID, "답변");

        assertThat(result.isEnded()).isFalse();
        verify(questionGenerator).generateFollowUpQuestion(session);
    }
}
