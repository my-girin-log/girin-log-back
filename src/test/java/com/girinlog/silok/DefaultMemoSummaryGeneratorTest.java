package com.girinlog.silok;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.girinlog.memo.domain.Memo;
import com.girinlog.memo.service.MemoSummaryCandidate;
import com.girinlog.silok.llm.GeminiProperties;
import com.girinlog.silok.llm.LlmProvider;
import com.girinlog.silok.llm.LlmResponse;
import com.girinlog.silok.llm.LlmUsage;
import com.girinlog.silok.llm.LlmFailureReason;
import com.girinlog.silok.llm.SilokLlmException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultMemoSummaryGeneratorTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 6, 9);
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-09T09:00:00+09:00");

    @Test
    @DisplayName("Gemini LlmClient JSON 응답을 MemoSummaryCandidate로 변환한다")
    void generate_parses_json_response() {
        AtomicReference<com.girinlog.silok.llm.LlmRequest> capturedRequest = new AtomicReference<>();
        DefaultMemoSummaryGenerator generator = generator(request -> {
            capturedRequest.set(request);
            return new LlmResponse("""
                    {
                      "memoSummaries": [
                        {
                          "categoryName": "학습",
                          "summary": "테스트 격리 방식을 익혔다.",
                          "items": [
                            {"memoId": 10, "content": "Testcontainers 실패 원인을 분리했다."}
                          ]
                        }
                      ]
                    }
                    """, "gemini-2.0-flash", LlmUsage.unknown());
        });

        List<MemoSummaryCandidate> candidates = generator.generate(List.of(memo(10L, "테스트 격리 고민")));

        assertThat(capturedRequest.get().provider()).isEqualTo(LlmProvider.GEMINI);
        assertThat(capturedRequest.get().model()).isEqualTo("gemini-2.0-flash");
        assertThat(candidates).hasSize(1);
        assertThat(candidates.getFirst().categoryName()).isEqualTo("학습");
        assertThat(candidates.getFirst().items().getFirst().memoId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("입력 Memo에 없는 memoId가 오면 INVALID_RESPONSE로 거부한다")
    void generate_rejects_unknown_memo_id() {
        DefaultMemoSummaryGenerator generator = generator(request -> new LlmResponse("""
                {
                  "memoSummaries": [
                    {
                      "categoryName": "학습",
                      "summary": "요약",
                      "items": [
                        {"memoId": 999, "content": "없는 Memo"}
                      ]
                    }
                  ]
                }
                """, "gemini-2.0-flash", LlmUsage.unknown()));

        assertThatThrownBy(() -> generator.generate(List.of(memo(10L, "테스트 격리 고민"))))
                .isInstanceOfSatisfying(SilokLlmException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(LlmFailureReason.INVALID_RESPONSE));
    }

    private DefaultMemoSummaryGenerator generator(com.girinlog.silok.llm.LlmClient llmClient) {
        GeminiProperties properties = new GeminiProperties();
        properties.setModel("gemini-2.0-flash");
        return new DefaultMemoSummaryGenerator(llmClient, properties, new ObjectMapper());
    }

    private Memo memo(Long id, String content) {
        Memo memo = Memo.draft(USER_ID, SERVICE_DATE, content, CREATED_AT);
        ReflectionTestUtils.setField(memo, "id", id);
        return memo;
    }
}
