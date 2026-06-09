package com.girinlog.silok.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class GeminiLlmClientTest {

    @Test
    @DisplayName("Gemini generateContent를 JSON 응답 모드로 호출한다")
    void generate_calls_gemini_with_json_response_format() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiLlmClient client = new GeminiLlmClient(properties(), builder.build());

        server.expect(once(), requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"))
                .andExpect(method(POST))
                .andExpect(header("x-goog-api-key", "test-api-key"))
                .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
                .andExpect(jsonPath("$.generationConfig.maxOutputTokens").value(2048))
                .andExpect(jsonPath("$.systemInstruction.parts[0].text").value("너는 실록이야."))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value("메모를 요약해줘."))
                .andRespond(withSuccess("""
                        {
                          "candidates": [
                            {
                              "content": {
                                "parts": [
                                  {"text": "{\\"memoSummaries\\":[{\\"categoryName\\":\\"학습\\"}]}"}
                                ]
                              }
                            }
                          ],
                          "usageMetadata": {
                            "promptTokenCount": 10,
                            "candidatesTokenCount": 20
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        LlmResponse response = client.generate(sampleRequest());

        assertThat(response.content()).contains("memoSummaries");
        assertThat(response.usage()).isEqualTo(LlmUsage.known(10, 20));
        server.verify();
    }

    @Test
    @DisplayName("GEMINI_API_KEY가 없으면 인증 실패로 변환한다")
    void missing_api_key_fails_with_authentication_reason() {
        GeminiProperties properties = properties();
        properties.setApiKey("");
        GeminiLlmClient client = new GeminiLlmClient(properties, RestClient.create());

        assertThatThrownBy(() -> client.generate(sampleRequest()))
                .isInstanceOfSatisfying(SilokLlmException.class, exception ->
                        assertThat(exception.reason()).isEqualTo(LlmFailureReason.AUTHENTICATION_FAILED));
    }

    private LlmRequest sampleRequest() {
        return LlmRequest.of(
                LlmProvider.GEMINI,
                "gemini-2.0-flash",
                "너는 실록이야.",
                "메모를 요약해줘."
        );
    }

    private GeminiProperties properties() {
        GeminiProperties properties = new GeminiProperties();
        properties.setApiKey("test-api-key");
        properties.setBaseUrl("https://generativelanguage.googleapis.com");
        properties.setModel("gemini-2.0-flash");
        return properties;
    }
}
