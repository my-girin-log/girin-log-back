package com.girinlog.memo;

import com.girinlog.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum MemoErrorCode implements ErrorCode {

    NO_SUMMARIZABLE_MEMO(HttpStatus.UNPROCESSABLE_ENTITY, "요약할 작성 중 Memo가 없습니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    MemoErrorCode(HttpStatus status, String defaultMessage) {
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
