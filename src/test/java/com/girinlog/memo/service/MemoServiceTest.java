package com.girinlog.memo.service;

import com.girinlog.common.error.BusinessException;
import com.girinlog.common.time.ServiceClockConfig;
import com.girinlog.event.domain.EventType;
import com.girinlog.event.service.EventLogRecorder;
import com.girinlog.memo.MemoErrorCode;
import com.girinlog.memo.domain.Memo;
import com.girinlog.memo.domain.MemoStatus;
import com.girinlog.memo.repository.MemoRepository;
import com.girinlog.memo.repository.MemoSummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MemoServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate JUNE_7 = LocalDate.of(2026, 6, 7);
    private static final LocalDate JUNE_8 = LocalDate.of(2026, 6, 8);

    @Mock
    private MemoRepository memoRepository;

    @Mock
    private MemoSummaryRepository memoSummaryRepository;

    @Mock
    private MemoSummaryGenerator memoSummaryGenerator;

    @Mock
    private EventLogRecorder eventLogRecorder;

    private MemoService memoService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-08T20:00:00Z"), ServiceClockConfig.KST);
        memoService = new MemoService(memoRepository, memoSummaryRepository, memoSummaryGenerator, eventLogRecorder, clock);
    }

    @Test
    @DisplayName("Memo 생성은 06:00 KST 경계 기준 서비스 날짜를 사용한다")
    void create_memo_uses_service_day_with_six_am_kst_boundary() {
        given(memoRepository.save(any(Memo.class))).willAnswer(invocation -> invocation.getArgument(0));

        Memo memo = memoService.createMemo(USER_ID, "오늘 회의 기록");

        assertThat(memo.serviceDate()).isEqualTo(JUNE_8);
        assertThat(memo.status()).isEqualTo(MemoStatus.DRAFT);
        then(eventLogRecorder).should().record(eq(USER_ID), eq(EventType.MEMO_CREATED), any());
    }

    @Test
    @DisplayName("요약할 DRAFT Memo가 없으면 NO_SUMMARIZABLE_MEMO로 거부한다")
    void create_memo_summaries_requires_draft_memo() {
        given(memoRepository.findByUserIdAndServiceDateAndStatusOrderByCreatedAtAsc(USER_ID, JUNE_8, MemoStatus.DRAFT))
                .willReturn(List.of());

        assertThatThrownBy(() -> memoService.createMemoSummaries(USER_ID, JUNE_8))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(MemoErrorCode.NO_SUMMARIZABLE_MEMO));
    }

    @Test
    @DisplayName("MemoSummary 생성은 원본을 SUMMARIZED로 바꾸고 다음 DRAFT Memo를 만든다")
    void create_memo_summaries_summarizes_sources_and_creates_next_draft_memo() {
        Memo sourceMemo = Memo.draft(USER_ID, JUNE_8, "오늘 배운 내용", memoServiceNow());
        given(memoRepository.findByUserIdAndServiceDateAndStatusOrderByCreatedAtAsc(USER_ID, JUNE_8, MemoStatus.DRAFT))
                .willReturn(List.of(sourceMemo));
        given(memoSummaryGenerator.generate(List.of(sourceMemo)))
                .willReturn(List.of(new MemoSummaryCandidate(
                        "학습",
                        "오늘 배운 내용",
                        List.of(new MemoSummaryItemCandidate(null, "오늘 배운 내용"))
                )));
        given(memoSummaryRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(memoRepository.save(any(Memo.class))).willAnswer(invocation -> invocation.getArgument(0));

        MemoSummaryCreation creation = memoService.createMemoSummaries(USER_ID, JUNE_8);

        assertThat(sourceMemo.status()).isEqualTo(MemoStatus.SUMMARIZED);
        assertThat(creation.memoSummaries()).hasSize(1);
        assertThat(creation.nextMemo().status()).isEqualTo(MemoStatus.DRAFT);
        assertThat(creation.nextMemo().content()).isEmpty();
        then(eventLogRecorder).should().record(eq(USER_ID), eq(EventType.MEMO_SUMMARIZED), any());
    }

    @Test
    @DisplayName("요약 생성기가 실패하면 원본 Memo는 DRAFT 상태를 유지한다")
    void create_memo_summaries_keeps_sources_draft_when_generator_fails() {
        Memo sourceMemo = Memo.draft(USER_ID, JUNE_8, "오늘 배운 내용", memoServiceNow());
        given(memoRepository.findByUserIdAndServiceDateAndStatusOrderByCreatedAtAsc(USER_ID, JUNE_8, MemoStatus.DRAFT))
                .willReturn(List.of(sourceMemo));
        given(memoSummaryGenerator.generate(List.of(sourceMemo)))
                .willThrow(new IllegalStateException("LLM 요약 실패"));

        assertThatThrownBy(() -> memoService.createMemoSummaries(USER_ID, JUNE_8))
                .isInstanceOf(IllegalStateException.class);
        assertThat(sourceMemo.status()).isEqualTo(MemoStatus.DRAFT);
    }

    private java.time.OffsetDateTime memoServiceNow() {
        return java.time.OffsetDateTime.parse("2026-06-08T09:00:00+09:00");
    }
}
