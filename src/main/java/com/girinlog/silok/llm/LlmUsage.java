package com.girinlog.silok.llm;

public record LlmUsage(
        boolean known,
        int inputTokens,
        int outputTokens
) {

    public LlmUsage {
        if (inputTokens < 0 || outputTokens < 0) {
            throw new IllegalArgumentException("token 수는 음수일 수 없습니다.");
        }
        if (!known && (inputTokens != 0 || outputTokens != 0)) {
            throw new IllegalArgumentException("알 수 없는 usage는 token 수를 0으로 둡니다.");
        }
    }

    public static LlmUsage known(int inputTokens, int outputTokens) {
        return new LlmUsage(true, inputTokens, outputTokens);
    }

    public static LlmUsage unknown() {
        return new LlmUsage(false, 0, 0);
    }
}
