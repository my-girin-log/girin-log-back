package com.girinlog.silok;

import com.girinlog.common.error.ErrorResponse;
import com.girinlog.silok.llm.SilokLlmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * silok LLM 호출 실패({@link SilokLlmException})를 고정 envelope({@link ErrorResponse}) 로 변환한다.
 *
 * <p>폴백이 없는 생성기(요약·대화·Diary·회고)에서 LLM 실패가 그대로 전파되면 공통 핸들러가 500
 * INTERNAL_ERROR 로 노출했다. 이 핸들러는 {@code SilokLlmException} 에 한해 더 구체적으로 매칭되어
 * 그보다 우선하며(Spring @ExceptionHandler 우선순위), 사유별 재시도 가능 상태코드로 응답한다.
 * Persona 생성기는 내부에서 graceful 폴백하므로 이 핸들러까지 오지 않는다.
 */
@RestControllerAdvice
public class SilokLlmExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SilokLlmExceptionHandler.class);

    @ExceptionHandler(SilokLlmException.class)
    public ResponseEntity<ErrorResponse> handleSilokLlm(SilokLlmException exception) {
        SilokErrorCode errorCode = SilokErrorCode.from(exception.reason());
        // 기존엔 로그 없이 500 으로 묻혔다. 원인 파악을 위해 사유·매핑 결과와 함께 남긴다.
        log.warn("silok LLM 실패 reason={} → {} {}",
                exception.reason(), errorCode.status(), errorCode.code(), exception);
        return ResponseEntity
                .status(errorCode.status())
                .body(ErrorResponse.of(errorCode.code(), errorCode.defaultMessage()));
    }
}
