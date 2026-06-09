package com.girinlog.memo.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemoTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 6, 8);
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-08T09:00:00+09:00");

    @Test
    @DisplayName("DRAFT 상태의 Memo는 내용을 수정할 수 있다")
    void draft_memo_can_update_content() {
        Memo memo = Memo.draft(USER_ID, SERVICE_DATE, "처음 기록", CREATED_AT);
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-06-08T10:00:00+09:00");

        memo.updateContent("수정한 기록", updatedAt);

        assertThat(memo.content()).isEqualTo("수정한 기록");
        assertThat(memo.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("SUMMARIZED 상태의 Memo는 내용을 수정할 수 없다")
    void summarized_memo_cannot_update_content() {
        Memo memo = Memo.draft(USER_ID, SERVICE_DATE, "처음 기록", CREATED_AT);
        memo.summarize();

        assertThatThrownBy(() -> memo.updateContent("수정한 기록", CREATED_AT))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("MemoSummary는 대화에 사용되면 재선택할 수 없도록 비활성화된다")
    void memo_summary_disables_chat_when_used_in_conversation() {
        MemoSummary memoSummary = MemoSummary.create(
                USER_ID,
                SERVICE_DATE,
                "학습",
                "오늘 배운 내용",
                java.util.List.of(new MemoSummaryItem(1L, "오늘 배운 내용")),
                CREATED_AT
        );

        memoSummary.disableChat();

        assertThat(memoSummary.chatAvailable()).isFalse();
        assertThat(memoSummary.chatDisabledReason()).isEqualTo(MemoSummaryChatDisabledReason.ALREADY_CHATTED);
    }
}
