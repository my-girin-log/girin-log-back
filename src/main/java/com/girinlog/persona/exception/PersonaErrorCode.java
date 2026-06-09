package com.girinlog.persona.exception;

import com.girinlog.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum PersonaErrorCode implements ErrorCode {

    PERSONA_NOT_FOUND(HttpStatus.NOT_FOUND, "아직 생성된 Persona가 없습니다."),
    ONBOARDING_PERSIST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "온보딩 처리 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    PersonaErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
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
