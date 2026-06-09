package com.girinlog.silok;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.girinlog.conversation.domain.ConversationTurn;
import com.girinlog.conversation.domain.DailyChatSession;
import com.girinlog.conversation.domain.EndedReason;
import com.girinlog.conversation.service.DailyChatQuestionGenerator;
import com.girinlog.memo.domain.MemoSummary;
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
import java.util.List;
import java.util.stream.Collectors;

@Component
class DefaultDailyChatQuestionGenerator implements DailyChatQuestionGenerator {

    private static final String SYSTEM_PROMPT = """
            너는 우테코 크루의 옆자리 동료 같은 AI 캐릭터 실록이다.
            상담사처럼 진단하지 말고, 담백하게 하루 기록을 더 잘 떠올리도록 한 번에 하나만 묻는다.
            질문은 감정, 이유, 판단 기준, 배운 점, 다음 행동 중 하나 이상의 정보를 끌어내야 한다.
            응답은 반드시 아래 JSON 구조만 반환한다.
            {"content":"실록이의 한 문장"}
            content는 비워두지 않는다.
            """;

    private final LlmClient llmClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;

    DefaultDailyChatQuestionGenerator(
            LlmClient llmClient,
            GeminiProperties geminiProperties,
            ObjectMapper objectMapper
    ) {
        this.llmClient = llmClient;
        this.geminiProperties = geminiProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String generateFirstQuestion(List<MemoSummary> memoSummaries) {
        return generate("""
                선택된 MemoSummary를 읽고 첫 역질문을 만들어라.

                %s
                """.formatted(summaryPrompt(memoSummaries)));
    }

    @Override
    public String generateFollowUpQuestion(DailyChatSession session) {
        return generate("""
                지금까지의 conversation을 읽고 다음 역질문을 만들어라.
                이미 물은 내용을 반복하지 말고, 사용자의 답변에서 더 구체화할 지점을 하나만 골라라.

                followUpCount: %d
                maxFollowUpCount: %d

                conversation:
                %s
                """.formatted(session.followUpCount(), session.maxFollowUpCount(), conversationPrompt(session)));
    }

    @Override
    public String generateClosingMessage(DailyChatSession session, EndedReason endedReason) {
        return generate("""
                대화 세션을 종료하는 짧은 마무리 멘트를 만들어라.
                새 질문을 하지 말고, 사용자가 남긴 맥락을 한 문장으로 받아줘라.

                endedReason: %s

                conversation:
                %s
                """.formatted(endedReason, conversationPrompt(session)));
    }

    private String generate(String userPrompt) {
        LlmResponse response = llmClient.generate(new LlmRequest(
                LlmProvider.GEMINI,
                geminiProperties.getModel(),
                SYSTEM_PROMPT,
                userPrompt,
                new LlmGenerationOptions(Duration.ofSeconds(30), LlmResponseFormat.JSON, 0.7, 512)
        ));
        return parse(response.content());
    }

    private String summaryPrompt(List<MemoSummary> memoSummaries) {
        return memoSummaries.stream()
                .map(memoSummary -> """
                        - memoSummaryId: %d
                          categoryName: %s
                          summary: %s
                          items:
                        %s
                        """.formatted(
                        memoSummary.id(),
                        memoSummary.categoryName(),
                        memoSummary.summary(),
                        itemPrompt(memoSummary)
                ))
                .collect(Collectors.joining("\n"));
    }

    private String itemPrompt(MemoSummary memoSummary) {
        return memoSummary.items().stream()
                .map(item -> "    - " + item.content())
                .collect(Collectors.joining("\n"));
    }

    private String conversationPrompt(DailyChatSession session) {
        return session.conversation().stream()
                .map(this::turnPrompt)
                .collect(Collectors.joining("\n"));
    }

    private String turnPrompt(ConversationTurn turn) {
        return "- %s: %s".formatted(turn.role(), turn.content());
    }

    private String parse(String json) {
        try {
            GeneratedDailyChatContent response = objectMapper.readValue(json, GeneratedDailyChatContent.class);
            if (response == null || response.content() == null || response.content().isBlank()) {
                throw invalidResponse("DailyChatSession JSON 응답의 content가 비어 있습니다.");
            }
            return response.content();
        } catch (JsonProcessingException exception) {
            throw new SilokLlmException(LlmFailureReason.INVALID_RESPONSE, "DailyChatSession JSON 응답 파싱에 실패했습니다.", exception);
        }
    }

    private SilokLlmException invalidResponse(String message) {
        return new SilokLlmException(LlmFailureReason.INVALID_RESPONSE, message);
    }

    private record GeneratedDailyChatContent(String content) {
    }
}
