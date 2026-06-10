package com.girinlog.retrospective.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetrospectiveTest {

    @Test
    @DisplayName("Retrospective는 기간과 원천 DailyChatSession 목록, Markdown을 가진다")
    void create_retrospective_has_period_source_sessions_and_markdown() {
        Retrospective retrospective = Retrospective.create(
                1L,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 8),
                List.of(10L, 11L),
                "첫 주 회고",
                "# 첫 주 회고",
                OffsetDateTime.parse("2026-06-09T09:00:00+09:00")
        );

        assertThat(retrospective.periodStart()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(retrospective.periodEnd()).isEqualTo(LocalDate.of(2026, 6, 8));
        assertThat(retrospective.sourceDailyChatSessionIds()).containsExactly(10L, 11L);
        assertThat(retrospective.markdown()).startsWith("# 첫 주 회고");
    }

    @Test
    @DisplayName("원천 DailyChatSession 없이 Retrospective를 만들 수 없다")
    void create_retrospective_requires_source_sessions() {
        assertThatThrownBy(() -> Retrospective.create(
                1L,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 8),
                List.of(),
                "첫 주 회고",
                "# 첫 주 회고",
                OffsetDateTime.parse("2026-06-09T09:00:00+09:00")
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
