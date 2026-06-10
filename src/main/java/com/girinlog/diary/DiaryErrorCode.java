package com.girinlog.diary;

import com.girinlog.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum DiaryErrorCode implements ErrorCode {

    DIARY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 날짜의 다이어리가 없습니다."),
    INVALID_DIARY_DATE_RANGE(HttpStatus.BAD_REQUEST, "조회 시작 날짜는 종료 날짜보다 늦을 수 없습니다."),
    INVALID_DIARY_CURSOR(HttpStatus.BAD_REQUEST, "Diary 커서가 올바르지 않습니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    DiaryErrorCode(HttpStatus status, String defaultMessage) {
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
