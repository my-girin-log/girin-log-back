package com.girinlog.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * 모든 에러 응답의 고정 envelope. (conventions/api.md 4절)
 *
 * <pre>
 * { "error": { "code": "...", "message": "...", "details": null } }
 * </pre>
 *
 * FE 는 이 구조 하나만 파싱한다.
 */
public record ErrorResponse(Body error) {

    public record Body(
            String code,
            String message,
            @JsonInclude(JsonInclude.Include.ALWAYS) Map<String, String> details) {
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new Body(code, message, null));
    }

    /** 필드별 검증 실패 등 부가 정보를 details(필드→사유)에 담는다. */
    public static ErrorResponse of(String code, String message, Map<String, String> details) {
        return new ErrorResponse(new Body(code, message, details));
    }
}
