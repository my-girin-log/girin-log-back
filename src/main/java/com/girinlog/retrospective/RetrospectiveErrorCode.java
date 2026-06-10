package com.girinlog.retrospective;

import com.girinlog.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum RetrospectiveErrorCode implements ErrorCode {

    RETROSPECTIVE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 회고를 찾을 수 없습니다."),
    INVALID_RETROSPECTIVE_DATE_RANGE(HttpStatus.BAD_REQUEST, "회고 시작 날짜는 종료 날짜보다 늦을 수 없습니다."),
    INVALID_RETROSPECTIVE_CURSOR(HttpStatus.BAD_REQUEST, "Retrospective 커서가 올바르지 않습니다."),
    NO_RETROSPECTIVE_SOURCE_SESSION(HttpStatus.UNPROCESSABLE_ENTITY, "선택한 기간에 회고를 생성할 대화가 없습니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    RetrospectiveErrorCode(HttpStatus status, String defaultMessage) {
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
