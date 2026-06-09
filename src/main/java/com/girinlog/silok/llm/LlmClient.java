package com.girinlog.silok.llm;

public interface LlmClient {

    LlmResponse generate(LlmRequest request);
}
