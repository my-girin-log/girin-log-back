package com.girinlog.diary.service;

import com.girinlog.common.error.BusinessException;
import com.girinlog.common.time.ServiceClockConfig;
import com.girinlog.conversation.domain.DailyChatSession;
import com.girinlog.conversation.domain.DailyChatSessionStatus;
import com.girinlog.conversation.repository.DailyChatSessionRepository;
import com.girinlog.diary.DiaryErrorCode;
import com.girinlog.diary.domain.Diary;
import com.girinlog.diary.repository.DiaryRepository;
import com.girinlog.event.domain.EventType;
import com.girinlog.event.service.EventLogRecorder;
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
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class DiaryServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 6, 8);
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-09T06:00:00+09:00");

    @Mock
    private DiaryRepository diaryRepository;

    @Mock
    private DailyChatSessionRepository dailyChatSessionRepository;

    @Mock
    private EventLogRecorder eventLogRecorder;

    private DiaryService diaryService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-08T21:00:00Z"), ServiceClockConfig.KST);
        diaryService = new DiaryService(
                diaryRepository,
                dailyChatSessionRepository,
                new StubDiaryGenerator(),
                eventLogRecorder,
                clock
        );
    }

    @Test
    @DisplayName("Diary 생성은 ENDED DailyChatSession만 입력으로 사용하고 이벤트를 기록한다")
    void generate_daily_diary_uses_ended_daily_chat_sessions() {
        DailyChatSession session = session(100L);
        given(diaryRepository.findByUserIdAndServiceDate(USER_ID, SERVICE_DATE)).willReturn(Optional.empty());
        given(dailyChatSessionRepository.findByUserIdAndServiceDateAndStatusOrderByCreatedAtAsc(
                USER_ID,
                SERVICE_DATE,
                DailyChatSessionStatus.ENDED
        )).willReturn(List.of(session));
        given(diaryRepository.save(any(Diary.class))).willAnswer(invocation -> {
            Diary diary = invocation.getArgument(0);
            ReflectionTestUtils.setField(diary, "id", 10L);
            return diary;
        });

        Optional<Diary> diary = diaryService.generateDailyDiary(USER_ID, SERVICE_DATE);

        assertThat(diary).isPresent();
        assertThat(diary.get().summary()).isEqualTo("하루 요약");
        assertThat(diary.get().createdAt()).isEqualTo(CREATED_AT);
        then(eventLogRecorder).should().record(eq(USER_ID), eq(EventType.DIARY_CREATED), any());
    }

    @Test
    @DisplayName("DailyChatSession이 없으면 빈 Diary를 만들지 않는다")
    void generate_daily_diary_returns_empty_when_no_daily_chat_session_exists() {
        given(diaryRepository.findByUserIdAndServiceDate(USER_ID, SERVICE_DATE)).willReturn(Optional.empty());
        given(dailyChatSessionRepository.findByUserIdAndServiceDateAndStatusOrderByCreatedAtAsc(
                USER_ID,
                SERVICE_DATE,
                DailyChatSessionStatus.ENDED
        )).willReturn(List.of());

        Optional<Diary> diary = diaryService.generateDailyDiary(USER_ID, SERVICE_DATE);

        assertThat(diary).isEmpty();
        then(diaryRepository).should(never()).save(any());
        then(eventLogRecorder).should(never()).record(any(), any(), any());
    }

    @Test
    @DisplayName("이미 Diary가 있으면 멱등하게 기존 Diary를 반환한다")
    void generate_daily_diary_returns_existing_diary_idempotently() {
        Diary existingDiary = diary(10L, SERVICE_DATE);
        given(diaryRepository.findByUserIdAndServiceDate(USER_ID, SERVICE_DATE))
                .willReturn(Optional.of(existingDiary));

        Optional<Diary> diary = diaryService.generateDailyDiary(USER_ID, SERVICE_DATE);

        assertThat(diary).contains(existingDiary);
        then(dailyChatSessionRepository).should(never())
                .findByUserIdAndServiceDateAndStatusOrderByCreatedAtAsc(any(), any(), any());
    }

    @Test
    @DisplayName("Diary 상세 조회에서 없으면 DIARY_NOT_FOUND로 거부한다")
    void get_diary_by_date_requires_existing_diary() {
        given(diaryRepository.findByUserIdAndServiceDate(USER_ID, SERVICE_DATE)).willReturn(Optional.empty());

        assertThatThrownBy(() -> diaryService.getDiaryByDate(USER_ID, SERVICE_DATE))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(DiaryErrorCode.DIARY_NOT_FOUND));
    }

    @Test
    @DisplayName("Diary 목록은 limit보다 하나 더 조회해 nextCursor를 만든다")
    void list_diaries_returns_next_cursor() {
        Diary latest = diary(2L, LocalDate.of(2026, 6, 9));
        Diary older = diary(1L, SERVICE_DATE);
        given(diaryRepository.findPageByDateRange(eq(USER_ID), isNull(), isNull(), isNull(), any(Pageable.class)))
                .willReturn(List.of(latest, older));

        DiaryPage page = diaryService.listDiaries(USER_ID, null, null, null, 1);

        assertThat(page.items()).containsExactly(latest);
        assertThat(page.nextCursor()).isNotBlank();
    }

    @Test
    @DisplayName("시작 날짜가 종료 날짜보다 늦으면 400 규칙 위반으로 거부한다")
    void list_diaries_rejects_invalid_date_range() {
        assertThatThrownBy(() -> diaryService.listDiaries(
                USER_ID,
                LocalDate.of(2026, 6, 9),
                SERVICE_DATE,
                null,
                20
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.errorCode()).isEqualTo(DiaryErrorCode.INVALID_DIARY_DATE_RANGE));
    }

    private DailyChatSession session(Long id) {
        DailyChatSession session = DailyChatSession.start(
                USER_ID,
                SERVICE_DATE,
                List.of(10L),
                "snapshot",
                "첫 질문",
                OffsetDateTime.parse("2026-06-08T09:00:00+09:00")
        );
        session.end(com.girinlog.conversation.domain.EndedReason.USER_ENDED, "마무리", CREATED_AT);
        ReflectionTestUtils.setField(session, "id", id);
        return session;
    }

    private Diary diary(Long id, LocalDate serviceDate) {
        Diary diary = Diary.create(
                USER_ID,
                serviceDate,
                "하루 요약",
                List.of("주요 사건"),
                "감정",
                "고민",
                "기준",
                "다음 행동",
                "# Diary",
                CREATED_AT
        );
        ReflectionTestUtils.setField(diary, "id", id);
        return diary;
    }

    private static class StubDiaryGenerator implements DiaryGenerator {

        @Override
        public DiaryContent generate(LocalDate serviceDate, List<DailyChatSession> sessions) {
            return new DiaryContent(
                    "하루 요약",
                    List.of("주요 사건"),
                    "감정",
                    "고민",
                    "기준",
                    "다음 행동",
                    "# Diary"
            );
        }
    }
}
