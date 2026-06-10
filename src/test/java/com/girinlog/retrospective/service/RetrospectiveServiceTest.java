package com.girinlog.retrospective.service;

import com.girinlog.common.error.BusinessException;
import com.girinlog.common.time.ServiceClockConfig;
import com.girinlog.conversation.domain.DailyChatSession;
import com.girinlog.conversation.domain.DailyChatSessionStatus;
import com.girinlog.conversation.domain.EndedReason;
import com.girinlog.conversation.repository.DailyChatSessionRepository;
import com.girinlog.event.domain.EventType;
import com.girinlog.event.service.EventLogRecorder;
import com.girinlog.persona.service.PersonaService;
import com.girinlog.retrospective.RetrospectiveErrorCode;
import com.girinlog.retrospective.domain.Retrospective;
import com.girinlog.retrospective.repository.RetrospectiveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class RetrospectiveServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate START_DATE = LocalDate.of(2026, 6, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 6, 8);
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-09T09:00:00+09:00");

    @Mock
    private RetrospectiveRepository retrospectiveRepository;

    @Mock
    private DailyChatSessionRepository dailyChatSessionRepository;

    @Mock
    private PersonaService personaService;

    @Mock
    private EventLogRecorder eventLogRecorder;

    private RetrospectiveService retrospectiveService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-09T00:00:00Z"), ServiceClockConfig.KST);
        retrospectiveService = new RetrospectiveService(
                retrospectiveRepository,
                dailyChatSessionRepository,
                personaService,
                new StubRetrospectiveGenerator(),
                eventLogRecorder,
                clock
        );
    }

    @Test
    @DisplayName("Retrospective 생성은 기간 내 ENDED DailyChatSession과 persona.md를 입력으로 사용한다")
    void create_retrospective_uses_daily_chat_sessions_and_persona_markdown() {
        DailyChatSession session = session(100L, START_DATE);
        given(dailyChatSessionRepository.findByUserIdAndServiceDateBetweenAndStatusOrderByServiceDateAscCreatedAtAsc(
                USER_ID,
                START_DATE,
                END_DATE,
                DailyChatSessionStatus.ENDED
        )).willReturn(List.of(session));
        given(personaService.findPersonaMarkdown(USER_ID)).willReturn(Optional.of("persona.md"));
        given(retrospectiveRepository.save(any(Retrospective.class))).willAnswer(invocation -> {
            Retrospective retrospective = invocation.getArgument(0);
            ReflectionTestUtils.setField(retrospective, "id", 10L);
            return retrospective;
        });

        Retrospective retrospective = retrospectiveService.createRetrospective(USER_ID, START_DATE, END_DATE);

        assertThat(retrospective.sourceDailyChatSessionIds()).containsExactly(100L);
        assertThat(retrospective.title()).isEqualTo("첫 주 회고");
        assertThat(retrospective.createdAt()).isEqualTo(NOW);
        then(eventLogRecorder).should().record(eq(USER_ID), eq(EventType.RETROSPECTIVE_CREATED), any());
    }

    @Test
    @DisplayName("persona.md가 없어도 Retrospective 생성을 차단하지 않는다")
    void create_retrospective_allows_missing_persona_markdown() {
        DailyChatSession session = session(100L, START_DATE);
        given(dailyChatSessionRepository.findByUserIdAndServiceDateBetweenAndStatusOrderByServiceDateAscCreatedAtAsc(
                USER_ID,
                START_DATE,
                END_DATE,
                DailyChatSessionStatus.ENDED
        )).willReturn(List.of(session));
        given(personaService.findPersonaMarkdown(USER_ID)).willReturn(Optional.empty());
        given(retrospectiveRepository.save(any(Retrospective.class))).willAnswer(invocation -> invocation.getArgument(0));

        Retrospective retrospective = retrospectiveService.createRetrospective(USER_ID, START_DATE, END_DATE);

        assertThat(retrospective.markdown()).isEqualTo("# 첫 주 회고");
    }

    @Test
    @DisplayName("기간 내 DailyChatSession이 없으면 422로 거부한다")
    void create_retrospective_requires_source_sessions() {
        given(dailyChatSessionRepository.findByUserIdAndServiceDateBetweenAndStatusOrderByServiceDateAscCreatedAtAsc(
                USER_ID,
                START_DATE,
                END_DATE,
                DailyChatSessionStatus.ENDED
        )).willReturn(List.of());

        assertThatThrownBy(() -> retrospectiveService.createRetrospective(USER_ID, START_DATE, END_DATE))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(RetrospectiveErrorCode.NO_RETROSPECTIVE_SOURCE_SESSION));
    }

    @Test
    @DisplayName("Retrospective 상세 조회에서 없으면 RETROSPECTIVE_NOT_FOUND로 거부한다")
    void get_retrospective_requires_existing_retrospective() {
        given(retrospectiveRepository.findByIdAndUserId(10L, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> retrospectiveService.getRetrospective(USER_ID, 10L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(RetrospectiveErrorCode.RETROSPECTIVE_NOT_FOUND));
    }

    @Test
    @DisplayName("Retrospective 목록은 limit보다 하나 더 조회해 nextCursor를 만든다")
    void list_retrospectives_returns_next_cursor() {
        Retrospective latest = retrospective(2L);
        Retrospective older = retrospective(1L);
        given(retrospectiveRepository.findPage(eq(USER_ID), isNull(), any(Pageable.class)))
                .willReturn(List.of(latest, older));

        RetrospectivePage page = retrospectiveService.listRetrospectives(USER_ID, null, 1);

        assertThat(page.items()).containsExactly(latest);
        assertThat(page.nextCursor()).isNotBlank();
    }

    private DailyChatSession session(Long id, LocalDate serviceDate) {
        DailyChatSession session = DailyChatSession.start(
                USER_ID,
                serviceDate,
                List.of(10L),
                "snapshot",
                "첫 질문",
                OffsetDateTime.parse("2026-06-01T09:00:00+09:00")
        );
        session.end(EndedReason.USER_ENDED, "마무리", NOW);
        ReflectionTestUtils.setField(session, "id", id);
        return session;
    }

    private Retrospective retrospective(Long id) {
        Retrospective retrospective = Retrospective.create(
                USER_ID,
                START_DATE,
                END_DATE,
                List.of(100L),
                "첫 주 회고",
                "# 첫 주 회고",
                NOW
        );
        ReflectionTestUtils.setField(retrospective, "id", id);
        return retrospective;
    }

    private static class StubRetrospectiveGenerator implements RetrospectiveGenerator {

        @Override
        public GeneratedRetrospective generate(
                LocalDate startDate,
                LocalDate endDate,
                List<DailyChatSession> sessions,
                Optional<String> personaMarkdown
        ) {
            return new GeneratedRetrospective("첫 주 회고", "# 첫 주 회고");
        }
    }
}
