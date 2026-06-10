package com.girinlog.silok;

import com.girinlog.common.error.ErrorCode;
import com.girinlog.silok.llm.LlmFailureReason;
import org.springframework.http.HttpStatus;

/**
 * silok LLM 호출 실패를 클라이언트에 노출할 에러 코드. ({@link ErrorCode} 계약 구현)
 *
 * <p>폴백이 없는 생성기(요약·대화·Diary·회고)의 LLM 실패가 그대로 500 INTERNAL_ERROR 로
 * 노출되던 것을, {@link LlmFailureReason} 별 <b>재시도 가능</b> 상태코드로 매핑한다.
 * 클라이언트는 {@link #code()} 로만 분기한다.
 */
public enum SilokErrorCode implements ErrorCode {

    SILOK_LLM_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "AI 응답 요청이 많아 잠시 지연되고 있어요. 잠시 후 다시 시도해 주세요."),
    SILOK_LLM_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AI 응답이 지연되어 처리하지 못했어요. 잠시 후 다시 시도해 주세요."),
    SILOK_LLM_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "AI 응답을 처리하지 못했어요. 잠시 후 다시 시도해 주세요."),
    SILOK_LLM_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI 기능을 일시적으로 사용할 수 없어요. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String defaultMessage;

    SilokErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    /** LLM 실패 사유를 노출용 에러 코드로 매핑한다. 인증 실패는 서버 설정 문제이므로 사유를 감추고 일시 장애로 응답한다. */
    public static SilokErrorCode from(LlmFailureReason reason) {
        return switch (reason) {
            case RATE_LIMITED -> SILOK_LLM_RATE_LIMITED;
            case TIMEOUT -> SILOK_LLM_TIMEOUT;
            case INVALID_RESPONSE -> SILOK_LLM_INVALID_RESPONSE;
            case AUTHENTICATION_FAILED, PROVIDER_ERROR -> SILOK_LLM_UNAVAILABLE;
        };
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public String defaultMessage() {
        return defaultMessage;
    }
}
