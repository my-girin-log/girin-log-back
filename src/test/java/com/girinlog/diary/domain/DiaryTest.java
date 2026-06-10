package com.girinlog.diary.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiaryTest {

    @Test
    @DisplayName("Diary는 회고 생성용 요약과 사용자 노출 Markdown을 필수로 가진다")
    void create_diary_requires_summary_and_markdown() {
        Diary diary = Diary.create(
                1L,
                LocalDate.of(2026, 6, 8),
                "하루 요약",
                List.of("페어 프로그래밍"),
                "막막했지만 정리됐다.",
                "테스트 경계",
                "작게 검증하기",
                "다음엔 먼저 테스트를 쓴다.",
                "# 2026-06-08\n하루 요약",
                OffsetDateTime.parse("2026-06-09T06:00:00+09:00")
        );

        assertThat(diary.summary()).isEqualTo("하루 요약");
        assertThat(diary.mainEvents()).containsExactly("페어 프로그래밍");
        assertThat(diary.markdown()).startsWith("# 2026-06-08");
    }

    @Test
    @DisplayName("summary가 비어 있으면 Diary를 만들 수 없다")
    void create_diary_rejects_blank_summary() {
        assertThatThrownBy(() -> Diary.create(
                1L,
                LocalDate.of(2026, 6, 8),
                "",
                List.of(),
                null,
                null,
                null,
                null,
                "markdown",
                OffsetDateTime.parse("2026-06-09T06:00:00+09:00")
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
