package com.girinlog.conversation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DailyChatSessionTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-08T09:00:00+09:00");

    @Test
    @DisplayName("세션 시작 시 첫 역질문을 conversation에 저장하고 followUpCount를 1로 올린다")
    void start_adds_first_follow_up_question() {
        DailyChatSession session = DailyChatSession.start(
                1L,
                LocalDate.of(2026, 6, 8),
                List.of(10L, 11L),
                "snapshot",
                "그때 어떤 기준으로 판단했어?",
                NOW
        );

        assertThat(session.status()).isEqualTo(DailyChatSessionStatus.OPEN);
        assertThat(session.followUpCount()).isEqualTo(1);
        assertThat(session.conversation()).hasSize(1);
        assertThat(session.conversation().getFirst().role()).isEqualTo(ConversationRole.SILOK);
    }

    @Test
    @DisplayName("역질문은 최대 10회까지만 추가할 수 있다")
    void follow_up_question_cannot_exceed_max_count() {
        DailyChatSession session = DailyChatSession.start(
                1L,
                LocalDate.of(2026, 6, 8),
                List.of(10L),
                "snapshot",
                "첫 질문",
                NOW
        );
        ReflectionTestUtils.setField(session, "followUpCount", 10);

        assertThatThrownBy(() -> session.addSilokFollowUpQuestion("열한 번째 질문", NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("세션 종료는 마무리 멘트를 conversation과 closingMessage에 함께 저장한다")
    void end_adds_closing_message() {
        DailyChatSession session = DailyChatSession.start(
                1L,
                LocalDate.of(2026, 6, 8),
                List.of(10L),
                "snapshot",
                "첫 질문",
                NOW
        );

        session.end(EndedReason.USER_ENDED, "오늘 얘기는 여기까지 정리해둘게.", NOW);

        assertThat(session.status()).isEqualTo(DailyChatSessionStatus.ENDED);
        assertThat(session.endedReason()).isEqualTo(EndedReason.USER_ENDED);
        assertThat(session.closingMessage()).isEqualTo("오늘 얘기는 여기까지 정리해둘게.");
        assertThat(session.conversation().getLast().role()).isEqualTo(ConversationRole.SILOK);
    }
}
