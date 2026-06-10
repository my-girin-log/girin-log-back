package com.girinlog.silok;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.girinlog.persona.generation.GeneratedPersona;
import com.girinlog.persona.generation.PersonaGenerationInput;
import com.girinlog.silok.llm.GeminiProperties;
import com.girinlog.silok.llm.LlmClient;
import com.girinlog.silok.llm.LlmFailureReason;
import com.girinlog.silok.llm.LlmRequest;
import com.girinlog.silok.llm.LlmResponse;
import com.girinlog.silok.llm.SilokLlmException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultPersonaGeneratorTest {

    private final LlmClient llmClient = mock(LlmClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final PersonaGenerationInput input = new PersonaGenerationInput(
            null, null, null, List.of(new PersonaGenerationInput.SurveyAnswer(1, "순서대로 정리해요")));

    private GeminiProperties properties(String apiKey) {
        GeminiProperties properties = new GeminiProperties();
        properties.setApiKey(apiKey);
        return properties;
    }

    private DefaultPersonaGenerator generator(GeminiProperties properties) {
        return new DefaultPersonaGenerator(llmClient, properties, objectMapper);
    }

    @Test
    @DisplayName("API 키가 없으면 LLM을 호출하지 않고 기본 Persona로 폴백한다")
    void fallbackWhenNoApiKey() {
        GeneratedPersona persona = generator(properties(null)).generate(input);

        assertThat(persona.tone()).isNotBlank();
        assertThat(persona.summary()).isNotBlank();
        verify(llmClient, never()).generate(any());
    }

    @Test
    @DisplayName("LLM이 유효한 JSON을 주면 그대로 Persona로 파싱한다")
    void parsesValidJson() {
        String json = """
                {"tone":"분석적","thinkingStyle":"원인부터","recurringInterests":["성능","협업"],
                 "organizingHabit":"개요먼저","retrospectionCriteria":"배움","preferredStructure":"긴 글",
                 "summary":"분석적인 회고자","markdown":"# Persona"}
                """;
        when(llmClient.generate(any(LlmRequest.class)))
                .thenReturn(LlmResponse.withoutUsage(json, "gemini-2.0-flash"));

        GeneratedPersona persona = generator(properties("key")).generate(input);

        assertThat(persona.tone()).isEqualTo("분석적");
        assertThat(persona.recurringInterests()).containsExactly("성능", "협업");
        assertThat(persona.summary()).isEqualTo("분석적인 회고자");
    }

    @Test
    @DisplayName("LLM 호출이 실패하면 기본 Persona로 폴백한다(온보딩 비차단)")
    void fallbackWhenLlmFails() {
        when(llmClient.generate(any(LlmRequest.class)))
                .thenThrow(new SilokLlmException(LlmFailureReason.PROVIDER_ERROR, "down"));

        GeneratedPersona persona = generator(properties("key")).generate(input);

        assertThat(persona.tone()).isNotBlank();
        assertThat(persona.summary()).isNotBlank();
    }

    @Test
    @DisplayName("LLM이 깨진 JSON을 주면 폴백한다")
    void fallbackWhenInvalidJson() {
        when(llmClient.generate(any(LlmRequest.class)))
                .thenReturn(LlmResponse.withoutUsage("not-json", "gemini-2.0-flash"));

        GeneratedPersona persona = generator(properties("key")).generate(input);

        assertThat(persona.tone()).isNotBlank();
        Mockito.verify(llmClient).generate(any());
    }
}
