package com.girinlog.common.error;

import org.springframework.http.HttpStatus;

/**
 * 도메인 에러 코드의 계약. (conventions/api.md 4·5절)
 *
 * <p>클라이언트 분기는 {@link #code()} 로만 한다. 메시지 문자열로 분기하지 않는다.
 * 각 도메인은 이 인터페이스를 구현한 enum 으로 자신의 에러 코드를 정의한다.
 */
public interface ErrorCode {

    /** HTTP 상태코드. code 와 함께 쓴다(200 에 에러 바디를 싣지 않는다). */
    HttpStatus status();

    /** UPPER_SNAKE_CASE 도메인 에러 코드. 클라이언트 분기의 유일한 기준. */
    String code();

    /** 사람이 읽는 한국어 기본 메시지. UI 노출 가능. */
    String defaultMessage();
}
