package com.girinlog.silok.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmGenerationOptionsTest {

    @Test
    @DisplayName("LLM 요청 timeout은 양수여야 한다")
    void timeout_must_be_positive() {
        assertThatThrownBy(() -> new LlmGenerationOptions(Duration.ZERO, LlmResponseFormat.JSON, 0.7, 1_024))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("LLM 요청 temperature는 0 이상 2 이하여야 한다")
    void temperature_must_be_supported_range() {
        assertThatThrownBy(() -> new LlmGenerationOptions(Duration.ofSeconds(10), LlmResponseFormat.JSON, 2.1, 1_024))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
