package com.girinlog.memo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemoArchiveTest {

    private Memo draft() {
        return Memo.draft(1L, LocalDate.of(2026, 6, 9), "내용",
                OffsetDateTime.of(2026, 6, 9, 10, 0, 0, 0, ZoneOffset.ofHours(9)));
    }

    @Test
    @DisplayName("DRAFT Memo는 ARCHIVED로 전환된다")
    void archiveDraft() {
        Memo memo = draft();

        memo.archive();

        assertThat(memo.status()).isEqualTo(MemoStatus.ARCHIVED);
    }

    @Test
    @DisplayName("DRAFT가 아니면 아카이브할 수 없다")
    void cannotArchiveNonDraft() {
        Memo memo = draft();
        memo.summarize();

        assertThatThrownBy(memo::archive).isInstanceOf(IllegalStateException.class);
    }
}
