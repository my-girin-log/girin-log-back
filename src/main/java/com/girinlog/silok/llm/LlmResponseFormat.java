package com.girinlog.silok.llm;

public enum LlmResponseFormat {
    JSON("application/json"),
    TEXT("text/plain");

    private final String mimeType;

    LlmResponseFormat(String mimeType) {
        this.mimeType = mimeType;
    }

    public String mimeType() {
        return mimeType;
    }
}
