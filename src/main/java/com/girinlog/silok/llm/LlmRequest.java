package com.girinlog.silok.llm;

import java.util.Objects;

public record LlmRequest(
        LlmProvider provider,
        String model,
        String systemPrompt,
        String userPrompt,
        LlmGenerationOptions options
) {

    public LlmRequest {
        Objects.requireNonNull(provider, "provider는 필수입니다.");
        model = requireText(model, "model");
        systemPrompt = requireText(systemPrompt, "systemPrompt");
        userPrompt = requireText(userPrompt, "userPrompt");
        options = Objects.requireNonNull(options, "options는 필수입니다.");
    }

    public static LlmRequest of(
            LlmProvider provider,
            String model,
            String systemPrompt,
            String userPrompt
    ) {
        return new LlmRequest(provider, model, systemPrompt, userPrompt, LlmGenerationOptions.defaults());
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "은 비어 있을 수 없습니다.");
        }
        return value;
    }
}
