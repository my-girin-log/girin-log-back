package com.girinlog.silok.llm;

import java.time.Duration;

public record LlmGenerationOptions(
        Duration timeout,
        LlmResponseFormat responseFormat,
        double temperature,
        int maxOutputTokens
) {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final LlmResponseFormat DEFAULT_RESPONSE_FORMAT = LlmResponseFormat.JSON;
    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final int DEFAULT_MAX_OUTPUT_TOKENS = 2_048;

    public LlmGenerationOptions {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout은 양수여야 합니다.");
        }
        if (responseFormat == null) {
            throw new IllegalArgumentException("responseFormat은 필수입니다.");
        }
        if (temperature < 0 || temperature > 2) {
            throw new IllegalArgumentException("temperature는 0 이상 2 이하여야 합니다.");
        }
        if (maxOutputTokens <= 0) {
            throw new IllegalArgumentException("maxOutputTokens는 양수여야 합니다.");
        }
    }

    public static LlmGenerationOptions defaults() {
        return new LlmGenerationOptions(
                DEFAULT_TIMEOUT,
                DEFAULT_RESPONSE_FORMAT,
                DEFAULT_TEMPERATURE,
                DEFAULT_MAX_OUTPUT_TOKENS
        );
    }
}
