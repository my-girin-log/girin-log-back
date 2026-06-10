package com.girinlog.silok;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.girinlog.conversation.domain.ConversationTurn;
import com.girinlog.conversation.domain.DailyChatSession;
import com.girinlog.diary.service.DiaryContent;
import com.girinlog.diary.service.DiaryGenerator;
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
import java.util.stream.Collectors;

@Component
class DefaultDiaryGenerator implements DiaryGenerator {

    private static final String SYSTEM_PROMPT = """
            너는 우테코 크루의 하루 대화를 날짜별 Diary로 정리하는 실록이다.
            DailyChatSession conversation만 근거로 하루를 요약한다.
            원본 Memo나 MemoSummary를 직접 입력으로 받은 것처럼 쓰지 않는다.
            응답은 반드시 아래 JSON 구조만 반환한다.
            {"summary":"회고 생성용 하루 요약","mainEvents":["주요 사건"],"emotionContext":"감정과 맥락","concerns":"고민한 지점","newCriteria":"새로 생긴 기준","nextActions":"다음 행동","markdown":"사용자에게 보여줄 Markdown"}
            summary와 markdown은 비워두지 않는다.
            """;

    private final LlmClient llmClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;

    DefaultDiaryGenerator(
            LlmClient llmClient,
            GeminiProperties geminiProperties,
            ObjectMapper objectMapper
    ) {
        this.llmClient = llmClient;
        this.geminiProperties = geminiProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public DiaryContent generate(LocalDate serviceDate, List<DailyChatSession> sessions) {
        LlmResponse response = llmClient.generate(new LlmRequest(
                LlmProvider.GEMINI,
                geminiProperties.getModel(),
                SYSTEM_PROMPT,
                userPrompt(serviceDate, sessions),
                new LlmGenerationOptions(Duration.ofSeconds(45), LlmResponseFormat.JSON, 0.5, 2048)
        ));
        return parse(response.content());
    }

    private String userPrompt(LocalDate serviceDate, List<DailyChatSession> sessions) {
        return """
                serviceDate: %s

                dailyChatSessions:
                %s
                """.formatted(serviceDate, sessionPrompt(sessions));
    }

    private String sessionPrompt(List<DailyChatSession> sessions) {
        return sessions.stream()
                .map(session -> """
                        - sessionId: %d
                          conversation:
                        %s
                        """.formatted(session.id(), conversationPrompt(session)))
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

    private DiaryContent parse(String json) {
        try {
            GeneratedDiaryContent response = objectMapper.readValue(json, GeneratedDiaryContent.class);
            return response.toDiaryContent();
        } catch (JsonProcessingException exception) {
            throw new SilokLlmException(LlmFailureReason.INVALID_RESPONSE, "Diary JSON 응답 파싱에 실패했습니다.", exception);
        }
    }

    private static SilokLlmException invalidResponse(String message) {
        return new SilokLlmException(LlmFailureReason.INVALID_RESPONSE, message);
    }

    private record GeneratedDiaryContent(
            String summary,
            List<String> mainEvents,
            String emotionContext,
            String concerns,
            String newCriteria,
            String nextActions,
            String markdown
    ) {

        DiaryContent toDiaryContent() {
            if (isBlank(summary) || isBlank(markdown)) {
                throw invalidResponse("Diary JSON 응답의 필수 필드가 비어 있습니다.");
            }
            return new DiaryContent(
                    summary,
                    mainEvents == null ? List.of() : mainEvents,
                    emotionContext,
                    concerns,
                    newCriteria,
                    nextActions,
                    markdown
            );
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
