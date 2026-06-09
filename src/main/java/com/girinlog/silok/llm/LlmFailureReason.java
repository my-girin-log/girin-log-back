package com.girinlog.silok.llm;

public enum LlmFailureReason {
    TIMEOUT,
    AUTHENTICATION_FAILED,
    RATE_LIMITED,
    PROVIDER_ERROR,
    INVALID_RESPONSE
}
