package com.girinlog.auth.exception;

import com.girinlog.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * 인증/사용자 도메인 에러 코드. (conventions/api.md 4·5절)
 */
public enum AuthErrorCode implements ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    GITHUB_OAUTH_FAILED(HttpStatus.UNAUTHORIZED, "GitHub 로그인에 실패했습니다. 다시 시도해주세요."),
    OAUTH_STATE_MISMATCH(HttpStatus.UNAUTHORIZED, "OAuth state가 일치하지 않습니다. 다시 로그인해주세요.");

    private final HttpStatus status;
    private final String defaultMessage;

    AuthErrorCode(HttpStatus status, String defaultMessage) {
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
