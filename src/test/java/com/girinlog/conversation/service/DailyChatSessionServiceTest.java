package com.girinlog.conversation.service;

import com.girinlog.common.error.BusinessException;
import com.girinlog.common.time.ServiceClockConfig;
import com.girinlog.conversation.ConversationErrorCode;
import com.girinlog.conversation.domain.DailyChatSession;
import com.girinlog.conversation.domain.EndedReason;
import com.girinlog.conversation.repository.DailyChatSessionRepository;
import com.girinlog.memo.domain.MemoSummary;
import com.girinlog.memo.domain.MemoSummaryItem;
import com.girinlog.memo.repository.MemoSummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DailyChatSessionServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 6, 8);
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-08T09:00:00+09:00");

    @Mock
    private DailyChatSessionRepository dailyChatSessionRepository;

    @Mock
    private MemoSummaryRepository memoSummaryRepository;

    private DailyChatSessionService dailyChatSessionService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-08T00:00:00Z"), ServiceClockConfig.KST);
        dailyChatSessionService = new DailyChatSessionService(
                dailyChatSessionRepository,
                memoSummaryRepository,
                new StubDailyChatQuestionGenerator(),
                clock
        );
    }

    @Test
    @DisplayName("대화 세션 생성은 선택한 MemoSummary를 비활성화하고 첫 질문을 저장한다")
    void create_daily_chat_session_disables_selected_memo_summaries() {
        MemoSummary memoSummary = memoSummary(10L, SERVICE_DATE);
        given(memoSummaryRepository.findByUserIdAndIdIn(USER_ID, List.of(10L))).willReturn(List.of(memoSummary));
        given(dailyChatSessionRepository.save(any(DailyChatSession.class))).willAnswer(invocation -> {
            DailyChatSession session = invocation.getArgument(0);
            ReflectionTestUtils.setField(session, "id", 100L);
            return session;
        });

        DailyChatSession session = dailyChatSessionService.createDailyChatSession(USER_ID, List.of(10L));

        assertThat(session.id()).isEqualTo(100L);
        assertThat(session.memoSummaryIds()).containsExactly(10L);
        assertThat(session.followUpCount()).isEqualTo(1);
        assertThat(session.conversation().getFirst().content()).isEqualTo("첫 질문");
        assertThat(memoSummary.chatAvailable()).isFalse();
    }

    @Test
    @DisplayName("없는 MemoSummary를 선택하면 404로 거부한다")
    void create_daily_chat_session_requires_existing_memo_summary() {
        given(memoSummaryRepository.findByUserIdAndIdIn(USER_ID, List.of(10L))).willReturn(List.of());

        assertThatThrownBy(() -> dailyChatSessionService.createDailyChatSession(USER_ID, List.of(10L)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ConversationErrorCode.MEMO_SUMMARY_NOT_FOUND));
    }

    @Test
    @DisplayName("이미 대화에 사용된 MemoSummary는 다시 선택할 수 없다")
    void create_daily_chat_session_rejects_unavailable_memo_summary() {
        MemoSummary memoSummary = memoSummary(10L, SERVICE_DATE);
        memoSummary.disableChat();
        given(memoSummaryRepository.findByUserIdAndIdIn(USER_ID, List.of(10L))).willReturn(List.of(memoSummary));

        assertThatThrownBy(() -> dailyChatSessionService.createDailyChatSession(USER_ID, List.of(10L)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ConversationErrorCode.MEMO_SUMMARY_NOT_CHAT_AVAILABLE));
    }

    @Test
    @DisplayName("현재 서비스 날짜가 아닌 MemoSummary는 선택할 수 없다")
    void create_daily_chat_session_requires_current_service_date() {
        MemoSummary memoSummary = memoSummary(10L, LocalDate.of(2026, 6, 7));
        given(memoSummaryRepository.findByUserIdAndIdIn(USER_ID, List.of(10L))).willReturn(List.of(memoSummary));

        assertThatThrownBy(() -> dailyChatSessionService.createDailyChatSession(USER_ID, List.of(10L)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ConversationErrorCode.MEMO_SUMMARY_SERVICE_DATE_MISMATCH));
    }

    @Test
    @DisplayName("답변을 저장하면 다음 역질문을 conversation에 추가한다")
    void submit_answer_adds_user_answer_and_next_follow_up_question() {
        DailyChatSession session = session(100L);
        given(dailyChatSessionRepository.findByIdAndUserId(100L, USER_ID)).willReturn(Optional.of(session));

        DailyChatSession updated = dailyChatSessionService.submitAnswer(USER_ID, 100L, "그때는 조금 막막했어.");

        assertThat(updated.conversation()).hasSize(3);
        assertThat(updated.conversation().get(1).content()).isEqualTo("그때는 조금 막막했어.");
        assertThat(updated.conversation().get(2).content()).isEqualTo("다음 질문");
        assertThat(updated.followUpCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("이미 종료된 세션에는 답변을 추가할 수 없다")
    void submit_answer_rejects_ended_session() {
        DailyChatSession session = session(100L);
        session.end(EndedReason.USER_ENDED, "마무리", NOW);
        given(dailyChatSessionRepository.findByIdAndUserId(100L, USER_ID)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> dailyChatSessionService.submitAnswer(USER_ID, 100L, "답변"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ConversationErrorCode.DAILY_CHAT_SESSION_ALREADY_ENDED));
    }

    @Test
    @DisplayName("10번째 질문에 답하면 MAX_FOLLOWUP으로 자동 종료한다")
    void submit_answer_ends_session_when_max_follow_up_reached() {
        DailyChatSession session = session(100L);
        ReflectionTestUtils.setField(session, "followUpCount", 10);
        given(dailyChatSessionRepository.findByIdAndUserId(100L, USER_ID)).willReturn(Optional.of(session));

        DailyChatSession updated = dailyChatSessionService.submitAnswer(USER_ID, 100L, "마지막 답변");

        assertThat(updated.isEnded()).isTrue();
        assertThat(updated.endedReason()).isEqualTo(EndedReason.MAX_FOLLOWUP);
        assertThat(updated.closingMessage()).isEqualTo("마무리");
    }

    private DailyChatSession session(Long id) {
        DailyChatSession session = DailyChatSession.start(
                USER_ID,
                SERVICE_DATE,
                List.of(10L),
                "snapshot",
                "첫 질문",
                NOW
        );
        ReflectionTestUtils.setField(session, "id", id);
        return session;
    }

    private MemoSummary memoSummary(Long id, LocalDate serviceDate) {
        MemoSummaryItem item = new MemoSummaryItem(1L, "오늘 배운 내용");
        MemoSummary memoSummary = MemoSummary.create(
                USER_ID,
                serviceDate,
                "학습",
                "오늘 배운 내용",
                List.of(item),
                NOW
        );
        ReflectionTestUtils.setField(memoSummary, "id", id);
        return memoSummary;
    }

    private static class StubDailyChatQuestionGenerator implements DailyChatQuestionGenerator {

        @Override
        public String generateFirstQuestion(List<MemoSummary> memoSummaries) {
            return "첫 질문";
        }

        @Override
        public String generateFollowUpQuestion(DailyChatSession session) {
            return "다음 질문";
        }

        @Override
        public String generateClosingMessage(DailyChatSession session, EndedReason endedReason) {
            return "마무리";
        }
    }
}
