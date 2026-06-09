package com.girinlog.conversation;

import com.girinlog.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ConversationErrorCode implements ErrorCode {

    DAILY_CHAT_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "대화 세션을 찾을 수 없습니다."),
    MEMO_SUMMARY_NOT_FOUND(HttpStatus.NOT_FOUND, "선택한 MemoSummary를 찾을 수 없습니다."),
    MEMO_SUMMARY_NOT_CHAT_AVAILABLE(HttpStatus.UNPROCESSABLE_ENTITY, "이미 대화에 사용된 MemoSummary는 다시 선택할 수 없습니다."),
    MEMO_SUMMARY_SERVICE_DATE_MISMATCH(HttpStatus.UNPROCESSABLE_ENTITY, "서로 다른 날짜의 MemoSummary는 한 대화 세션에 함께 선택할 수 없습니다."),
    DAILY_CHAT_SESSION_ALREADY_ENDED(HttpStatus.CONFLICT, "이미 종료된 대화 세션입니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    ConversationErrorCode(HttpStatus status, String defaultMessage) {
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
