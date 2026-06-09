package com.girinlog.silok.llm;

import java.util.Objects;

public record LlmResponse(
        String content,
        String model,
        LlmUsage usage
) {

    public LlmResponse {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content는 비어 있을 수 없습니다.");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("model은 비어 있을 수 없습니다.");
        }
        usage = Objects.requireNonNull(usage, "usage는 필수입니다.");
    }

    public static LlmResponse withoutUsage(String content, String model) {
        return new LlmResponse(content, model, LlmUsage.unknown());
    }
}
