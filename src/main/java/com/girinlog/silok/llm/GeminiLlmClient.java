package com.girinlog.silok.llm;

import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Objects;

public class GeminiLlmClient implements LlmClient {

    private static final String GENERATE_CONTENT_PATH = "/v1beta/models/{model}:generateContent";

    private final GeminiProperties properties;
    private final RestClient restClient;

    public GeminiLlmClient(GeminiProperties properties, RestClient restClient) {
        this.properties = Objects.requireNonNull(properties, "properties는 필수입니다.");
        this.restClient = Objects.requireNonNull(restClient, "restClient는 필수입니다.");
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        if (request.provider() != LlmProvider.GEMINI) {
            throw new SilokLlmException(LlmFailureReason.PROVIDER_ERROR, "Gemini LLM Client는 Gemini 요청만 처리합니다.");
        }
        if (!properties.hasApiKey()) {
            throw new SilokLlmException(LlmFailureReason.AUTHENTICATION_FAILED, "GEMINI_API_KEY가 설정되어 있지 않습니다.");
        }

        try {
            GeminiResponse response = restClient.post()
                    .uri(GENERATE_CONTENT_PATH, request.model())
                    .header("x-goog-api-key", properties.getApiKey())
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(GeminiRequest.from(request))
                    .retrieve()
                    .body(GeminiResponse.class);
            return toLlmResponse(request, response);
        } catch (HttpClientErrorException.TooManyRequests exception) {
            throw new SilokLlmException(LlmFailureReason.RATE_LIMITED, "Gemini 호출 한도를 초과했습니다.", exception);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            throw new SilokLlmException(LlmFailureReason.AUTHENTICATION_FAILED, "Gemini 인증에 실패했습니다.", exception);
        } catch (HttpClientErrorException | HttpServerErrorException exception) {
            throw new SilokLlmException(LlmFailureReason.PROVIDER_ERROR, "Gemini 호출에 실패했습니다.", exception);
        } catch (ResourceAccessException exception) {
            throw new SilokLlmException(LlmFailureReason.TIMEOUT, "Gemini 호출 시간이 초과되었습니다.", exception);
        }
    }

    private LlmResponse toLlmResponse(LlmRequest request, GeminiResponse response) {
        if (response == null) {
            throw new SilokLlmException(LlmFailureReason.INVALID_RESPONSE, "Gemini 응답이 비어 있습니다.");
        }

        String content = response.text();
        if (content == null || content.isBlank()) {
            throw new SilokLlmException(LlmFailureReason.INVALID_RESPONSE, "Gemini 응답 본문이 비어 있습니다.");
        }

        LlmUsage usage = response.usageMetadata() == null
                ? LlmUsage.unknown()
                : LlmUsage.known(response.usageMetadata().promptTokenCount(), response.usageMetadata().candidatesTokenCount());
        return new LlmResponse(content, request.model(), usage);
    }

    private record GeminiRequest(
            Content systemInstruction,
            List<Content> contents,
            GenerationConfig generationConfig
    ) {

        static GeminiRequest from(LlmRequest request) {
            return new GeminiRequest(
                    Content.user(request.systemPrompt()),
                    List.of(Content.user(request.userPrompt())),
                    GenerationConfig.from(request.options())
            );
        }
    }

    private record GenerationConfig(
            String responseMimeType,
            double temperature,
            int maxOutputTokens
    ) {

        static GenerationConfig from(LlmGenerationOptions options) {
            return new GenerationConfig(
                    options.responseFormat().mimeType(),
                    options.temperature(),
                    options.maxOutputTokens()
            );
        }
    }

    private record Content(
            String role,
            List<Part> parts
    ) {

        static Content user(String text) {
            return new Content("user", List.of(new Part(text)));
        }
    }

    private record Part(String text) {
    }

    private record GeminiResponse(
            List<Candidate> candidates,
            UsageMetadata usageMetadata
    ) {

        String text() {
            if (candidates == null || candidates.isEmpty()) {
                return null;
            }
            Content content = candidates.getFirst().content();
            if (content == null || content.parts() == null || content.parts().isEmpty()) {
                return null;
            }
            return content.parts().getFirst().text();
        }
    }

    private record Candidate(Content content) {
    }

    private record UsageMetadata(
            int promptTokenCount,
            int candidatesTokenCount
    ) {
    }
}
