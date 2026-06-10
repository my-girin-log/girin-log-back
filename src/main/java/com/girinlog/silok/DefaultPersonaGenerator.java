package com.girinlog.silok;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.girinlog.persona.generation.GeneratedPersona;
import com.girinlog.persona.generation.PersonaGenerationInput;
import com.girinlog.persona.generation.PersonaGenerator;
import com.girinlog.silok.llm.GeminiProperties;
import com.girinlog.silok.llm.LlmClient;
import com.girinlog.silok.llm.LlmFailureReason;
import com.girinlog.silok.llm.LlmProvider;
import com.girinlog.silok.llm.LlmRequest;
import com.girinlog.silok.llm.LlmResponse;
import com.girinlog.silok.llm.SilokLlmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Persona 생성기 — 실록이 LLM(Gemini)로 사용자 말투·회고 기준을 분석한다.
 * 포트({@link PersonaGenerator})는 persona 도메인 소유, 구현은 silok가 가진다(coding.md 8절).
 *
 * <p>온보딩이 LLM 사정으로 깨지면 안 되므로(API 키 없음·호출 실패), 그 경우 결정적 기본 Persona로
 * <b>graceful 폴백</b>한다(product 2-3: 일부 입력만으로도 생성 가능).
 */
@Component
class DefaultPersonaGenerator implements PersonaGenerator {

    private static final Logger log = LoggerFactory.getLogger(DefaultPersonaGenerator.class);

    private static final String SYSTEM_PROMPT = """
            너는 우테코 크루의 글과 설문을 읽고 회고용 Persona를 정리하는 실록이다.
            아래 JSON 구조만 반환한다. 다른 텍스트는 절대 포함하지 않는다.
            {"tone":"말투 특징","thinkingStyle":"사고 흐름","recurringInterests":["관심사"],"organizingHabit":"글 정리 습관","retrospectionCriteria":"회고 기준","preferredStructure":"선호 글 구조","summary":"회고 생성용 요약","markdown":"# Persona\\n..."}
            tone, thinkingStyle, summary, markdown은 비워두지 않는다. 한국어로 작성한다.
            """;

    private final LlmClient llmClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;

    DefaultPersonaGenerator(LlmClient llmClient, GeminiProperties geminiProperties, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.geminiProperties = geminiProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public GeneratedPersona generate(PersonaGenerationInput input) {
        if (!geminiProperties.hasApiKey()) {
            log.info("Gemini API 키가 없어 기본 Persona로 생성합니다.");
            return fallback(input);
        }
        try {
            LlmResponse response = llmClient.generate(LlmRequest.of(
                    LlmProvider.GEMINI,
                    geminiProperties.getModel(),
                    SYSTEM_PROMPT,
                    userPrompt(input)));
            return parse(response.content());
        } catch (SilokLlmException exception) {
            log.warn("Persona LLM 생성 실패({}) — 기본 Persona로 폴백합니다.", exception.reason(), exception);
            return fallback(input);
        }
    }

    private String userPrompt(PersonaGenerationInput input) {
        StringBuilder prompt = new StringBuilder();
        if (input.blogUrl() != null && !input.blogUrl().isBlank()) {
            prompt.append("블로그 링크: ").append(input.blogUrl()).append('\n');
        }
        if (input.rawText() != null && !input.rawText().isBlank()) {
            prompt.append("기존 글 원문:\n").append(input.rawText()).append('\n');
        }
        prompt.append("온보딩 설문 응답:\n");
        List<PersonaGenerationInput.SurveyAnswer> answers = input.answers();
        if (answers != null) {
            for (PersonaGenerationInput.SurveyAnswer answer : answers) {
                prompt.append("- Q").append(answer.questionId()).append(": ").append(answer.answer()).append('\n');
            }
        }
        return prompt.toString();
    }

    private GeneratedPersona parse(String json) {
        try {
            PersonaGenerationResponse response = objectMapper.readValue(json, PersonaGenerationResponse.class);
            return response.toGenerated();
        } catch (JsonProcessingException exception) {
            throw new SilokLlmException(LlmFailureReason.INVALID_RESPONSE, "Persona JSON 응답 파싱에 실패했습니다.", exception);
        }
    }

    /** LLM 사용 불가/실패 시 결정적 기본 Persona. 온보딩 완료를 막지 않는다. */
    private GeneratedPersona fallback(PersonaGenerationInput input) {
        int answerCount = input.answers() == null ? 0 : input.answers().size();
        boolean hasWriting = (input.blogUrl() != null && !input.blogUrl().isBlank())
                || (input.rawText() != null && !input.rawText().isBlank());
        String basis = hasWriting ? "기존 글과 설문" : "온보딩 설문";
        return new GeneratedPersona(
                "담백한",
                "사건 → 감정 → 판단 순서로 정리",
                List.of("회고", "학습"),
                "핵심부터 짧게 정리",
                "무엇을 배웠고 다음에 무엇을 다르게 할지",
                "짧고 담백한 글",
                basis + " " + answerCount + "문항을 바탕으로 한 기본 Persona 요약.",
                "# Persona\n\n- 말투: 담백한\n- 기준: 배운 점과 다음 행동\n");
    }

    private record PersonaGenerationResponse(
            String tone,
            String thinkingStyle,
            List<String> recurringInterests,
            String organizingHabit,
            String retrospectionCriteria,
            String preferredStructure,
            String summary,
            String markdown) {

        GeneratedPersona toGenerated() {
            if (isBlank(tone) || isBlank(thinkingStyle) || isBlank(summary)) {
                throw new SilokLlmException(
                        LlmFailureReason.INVALID_RESPONSE, "Persona JSON 응답의 필수 필드가 비어 있습니다.");
            }
            return new GeneratedPersona(
                    tone,
                    thinkingStyle,
                    recurringInterests == null ? List.of() : recurringInterests,
                    organizingHabit,
                    retrospectionCriteria,
                    preferredStructure,
                    summary,
                    markdown);
        }

        private static boolean isBlank(String value) {
            return value == null || value.isBlank();
        }
    }
}
