package com.girinlog.silok;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.girinlog.conversation.domain.ConversationTurn;
import com.girinlog.conversation.domain.DailyChatSession;
import com.girinlog.retrospective.service.GeneratedRetrospective;
import com.girinlog.retrospective.service.RetrospectiveGenerator;
import com.girinlog.silok.llm.GeminiProperties;
import com.girinlog.silok.llm.LlmClient;
import com.girinlog.silok.llm.LlmFailureReason;
import com.girinlog.silok.llm.LlmGenerationOptions;
import com.girinlog.silok.llm.LlmProvider;
import com.girinlog.silok.llm.LlmRequest;
import com.girinlog.silok.llm.LlmResponse;
import com.girinlog.silok.llm.LlmResponseFormat;
import com.girinlog.silok.llm.SilokLlmException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
class DefaultRetrospectiveGenerator implements RetrospectiveGenerator {

    private static final String SYSTEM_PROMPT = """
            너는 우테코 크루의 회고 초안을 작성하는 실록이다.
            선택 기간의 DailyChatSession conversation과 persona.md만 근거로 Retrospective를 생성한다.
            원본 Memo, MemoSummary, Diary를 직접 입력으로 받은 것처럼 쓰지 않는다.
            persona.md가 비어 있으면 대화 원문만으로 담백하게 작성한다.
            응답은 반드시 아래 JSON 구조만 반환한다.
            {"title":"회고 제목","markdown":"Markdown 본문"}
            markdown에는 도입, 주요 경험, 고민과 판단 기준, 배운 점, 다음에 다르게 해볼 점, 마무리가 포함되어야 한다.
            """;

    private final LlmClient llmClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;

    DefaultRetrospectiveGenerator(
            LlmClient llmClient,
            GeminiProperties geminiProperties,
            ObjectMapper objectMapper
    ) {
        this.llmClient = llmClient;
        this.geminiProperties = geminiProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public GeneratedRetrospective generate(
            LocalDate startDate,
            LocalDate endDate,
            List<DailyChatSession> sessions,
            Optional<String> personaMarkdown
    ) {
        LlmResponse response = llmClient.generate(new LlmRequest(
                LlmProvider.GEMINI,
                geminiProperties.getModel(),
                SYSTEM_PROMPT,
                userPrompt(startDate, endDate, sessions, personaMarkdown),
                new LlmGenerationOptions(Duration.ofSeconds(60), LlmResponseFormat.JSON, 0.6, 4096)
        ));
        return parse(response.content());
    }

    private String userPrompt(
            LocalDate startDate,
            LocalDate endDate,
            List<DailyChatSession> sessions,
            Optional<String> personaMarkdown
    ) {
        return """
                periodStart: %s
                periodEnd: %s

                persona.md:
                %s

                dailyChatSessions:
                %s
                """.formatted(
                startDate,
                endDate,
                personaMarkdown.orElse("(persona.md 없음)"),
                sessionPrompt(sessions)
        );
    }

    private String sessionPrompt(List<DailyChatSession> sessions) {
        return sessions.stream()
                .map(session -> """
                        - sessionId: %d
                          serviceDate: %s
                          conversation:
                        %s
                        """.formatted(session.id(), session.serviceDate(), conversationPrompt(session)))
                .collect(Collectors.joining("\n"));
    }

    private String conversationPrompt(DailyChatSession session) {
        return session.conversation().stream()
                .map(this::turnPrompt)
                .collect(Collectors.joining("\n"));
    }

    private String turnPrompt(ConversationTurn turn) {
        return "    - %s: %s".formatted(turn.role(), turn.content());
    }

    private GeneratedRetrospective parse(String json) {
        try {
            GeneratedRetrospectiveResponse response = objectMapper.readValue(json, GeneratedRetrospectiveResponse.class);
            return response.toGeneratedRetrospective();
        } catch (JsonProcessingException exception) {
            throw new SilokLlmException(LlmFailureReason.INVALID_RESPONSE, "Retrospective JSON 응답 파싱에 실패했습니다.", exception);
        }
    }

    private static SilokLlmException invalidResponse(String message) {
        return new SilokLlmException(LlmFailureReason.INVALID_RESPONSE, message);
    }

    private record GeneratedRetrospectiveResponse(
            String title,
            String markdown
    ) {

        GeneratedRetrospective toGeneratedRetrospective() {
            if (isBlank(title) || isBlank(markdown)) {
                throw invalidResponse("Retrospective JSON 응답의 필수 필드가 비어 있습니다.");
            }
            return new GeneratedRetrospective(title, markdown);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
