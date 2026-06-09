package com.girinlog.silok;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.girinlog.memo.domain.Memo;
import com.girinlog.memo.service.MemoSummaryCandidate;
import com.girinlog.memo.service.MemoSummaryGenerator;
import com.girinlog.memo.service.MemoSummaryItemCandidate;
import com.girinlog.silok.llm.GeminiProperties;
import com.girinlog.silok.llm.LlmClient;
import com.girinlog.silok.llm.LlmProvider;
import com.girinlog.silok.llm.LlmRequest;
import com.girinlog.silok.llm.LlmResponse;
import com.girinlog.silok.llm.LlmFailureReason;
import com.girinlog.silok.llm.SilokLlmException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
class DefaultMemoSummaryGenerator implements MemoSummaryGenerator {

    private static final String SYSTEM_PROMPT = """
            너는 우테코 크루의 하루 기록을 정리하는 실록이다.
            Memo 목록을 읽고 카테고리별 MemoSummary JSON만 반환한다.
            응답은 반드시 아래 JSON 구조를 따른다.
            {"memoSummaries":[{"categoryName":"카테고리명","summary":"요약","items":[{"memoId":1,"content":"요약 항목"}]}]}
            categoryName, summary, content는 비워두지 않는다.
            items[].memoId는 입력으로 받은 memoId 중 하나만 사용한다.
            """;

    private final LlmClient llmClient;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;

    DefaultMemoSummaryGenerator(
            LlmClient llmClient,
            GeminiProperties geminiProperties,
            ObjectMapper objectMapper
    ) {
        this.llmClient = llmClient;
        this.geminiProperties = geminiProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<MemoSummaryCandidate> generate(List<Memo> memos) {
        LlmResponse response = llmClient.generate(LlmRequest.of(
                LlmProvider.GEMINI,
                geminiProperties.getModel(),
                SYSTEM_PROMPT,
                userPrompt(memos)
        ));
        return parse(response.content(), memos);
    }

    private String userPrompt(List<Memo> memos) {
        return memos.stream()
                .map(memo -> "- memoId: %d%n  content: %s".formatted(memo.id(), memo.content()))
                .collect(Collectors.joining("\n"));
    }

    private List<MemoSummaryCandidate> parse(String json, List<Memo> sourceMemos) {
        try {
            MemoSummaryGenerationResponse response = objectMapper.readValue(json, MemoSummaryGenerationResponse.class);
            return toCandidates(response, sourceMemos);
        } catch (JsonProcessingException exception) {
            throw new SilokLlmException(LlmFailureReason.INVALID_RESPONSE, "MemoSummary JSON 응답 파싱에 실패했습니다.", exception);
        }
    }

    private List<MemoSummaryCandidate> toCandidates(
            MemoSummaryGenerationResponse response,
            List<Memo> sourceMemos
    ) {
        if (response == null || response.memoSummaries() == null || response.memoSummaries().isEmpty()) {
            throw invalidResponse("MemoSummary JSON 응답에 memoSummaries가 없습니다.");
        }

        Set<Long> sourceMemoIds = sourceMemos.stream()
                .map(Memo::id)
                .collect(Collectors.toSet());
        return response.memoSummaries().stream()
                .map(summary -> summary.toCandidate(sourceMemoIds))
                .toList();
    }

    private static SilokLlmException invalidResponse(String message) {
        return new SilokLlmException(LlmFailureReason.INVALID_RESPONSE, message);
    }

    private record MemoSummaryGenerationResponse(
            List<GeneratedMemoSummary> memoSummaries
    ) {
    }

    private record GeneratedMemoSummary(
            String categoryName,
            String summary,
            List<GeneratedMemoSummaryItem> items
    ) {

        MemoSummaryCandidate toCandidate(Set<Long> sourceMemoIds) {
            if (isBlank(categoryName) || isBlank(summary) || items == null || items.isEmpty()) {
                throw invalidResponse("MemoSummary JSON 응답의 필수 필드가 비어 있습니다.");
            }

            return new MemoSummaryCandidate(
                    categoryName,
                    summary,
                    items.stream()
                            .map(item -> item.toCandidate(sourceMemoIds))
                            .toList()
            );
        }
    }

    private record GeneratedMemoSummaryItem(
            Long memoId,
            String content
    ) {

        MemoSummaryItemCandidate toCandidate(Set<Long> sourceMemoIds) {
            if (memoId == null || !sourceMemoIds.contains(memoId) || isBlank(content)) {
                throw invalidResponse("MemoSummaryItem JSON 응답이 입력 Memo와 맞지 않습니다.");
            }
            return new MemoSummaryItemCandidate(memoId, content);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
