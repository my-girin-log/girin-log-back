package com.girinlog.silok.llm;

import java.util.Objects;

public class SilokLlmException extends RuntimeException {

    private final LlmFailureReason reason;

    public SilokLlmException(LlmFailureReason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "reason은 필수입니다.");
    }

    public SilokLlmException(LlmFailureReason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = Objects.requireNonNull(reason, "reason은 필수입니다.");
    }

    public LlmFailureReason reason() {
        return reason;
    }
}
