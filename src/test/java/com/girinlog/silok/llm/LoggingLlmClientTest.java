package com.girinlog.silok.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoggingLlmClientTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-09T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Test
    @DisplayName("위임 LlmClient의 응답을 그대로 반환한다")
    void returns_delegate_response() {
        LlmResponse expected = LlmResponse.withoutUsage("요약 결과", "gemini-2.0-flash");
        LoggingLlmClient client = new LoggingLlmClient(request -> expected, CLOCK);

        LlmResponse response = client.generate(sampleRequest());

        assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("위임 LlmClient의 일반 예외를 SilokLlmException으로 변환한다")
    void converts_runtime_exception_to_silok_llm_exception() {
        LoggingLlmClient client = new LoggingLlmClient(request -> {
            throw new IllegalStateException("provider error");
        }, CLOCK);

        assertThatThrownBy(() -> client.generate(sampleRequest()))
                .isInstanceOfSatisfying(SilokLlmException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(LlmFailureReason.PROVIDER_ERROR));
    }

    private LlmRequest sampleRequest() {
        return LlmRequest.of(
                LlmProvider.GEMINI,
                "gemini-2.0-flash",
                "너는 실록이야.",
                "오늘 기록을 요약해줘."
        );
    }
}
