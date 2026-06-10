package com.girinlog.silok;

import com.girinlog.common.error.ErrorResponse;
import com.girinlog.common.error.GlobalExceptionHandler;
import com.girinlog.silok.llm.LlmFailureReason;
import com.girinlog.silok.llm.SilokLlmException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SilokLlmExceptionHandlerTest {

    private final SilokLlmExceptionHandler handler = new SilokLlmExceptionHandler();

    @ParameterizedTest
    @CsvSource({
            "RATE_LIMITED,429,SILOK_LLM_RATE_LIMITED",
            "TIMEOUT,504,SILOK_LLM_TIMEOUT",
            "INVALID_RESPONSE,502,SILOK_LLM_INVALID_RESPONSE",
            "AUTHENTICATION_FAILED,503,SILOK_LLM_UNAVAILABLE",
            "PROVIDER_ERROR,503,SILOK_LLM_UNAVAILABLE"
    })
    @DisplayName("LLM 실패 사유를 재시도 가능한 상태코드와 에러 코드로 매핑한다")
    void maps_reason_to_status_and_code(LlmFailureReason reason, int status, String code) {
        ResponseEntity<ErrorResponse> response = handler.handleSilokLlm(new SilokLlmException(reason, "실패"));

        assertThat(response.getStatusCode().value()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo(code);
    }

    @ParameterizedTest
    @CsvSource({
            "RATE_LIMITED,429,SILOK_LLM_RATE_LIMITED",
            "TIMEOUT,504,SILOK_LLM_TIMEOUT",
            "INVALID_RESPONSE,502,SILOK_LLM_INVALID_RESPONSE",
            "PROVIDER_ERROR,503,SILOK_LLM_UNAVAILABLE"
    })
    @DisplayName("HTTP 경로에서 SilokLlmException 은 공통 500 핸들러보다 우선해 매핑된 상태로 응답한다")
    void silok_advice_wins_over_generic_500_handler(LlmFailureReason reason, int status, String code) throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ThrowingController(reason))
                // 공통 핸들러(Exception → 500)를 함께 등록해도 더 구체적인 silok 핸들러가 이긴다.
                .setControllerAdvice(new SilokLlmExceptionHandler(), new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/test/silok-fail"))
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.error.code").value(code));
    }

    @RestController
    static class ThrowingController {

        private final LlmFailureReason reason;

        ThrowingController(LlmFailureReason reason) {
            this.reason = reason;
        }

        @GetMapping("/test/silok-fail")
        String fail() {
            throw new SilokLlmException(reason, "LLM 실패");
        }
    }
}
