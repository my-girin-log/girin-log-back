package com.girinlog.common.error;

/**
 * 도메인 규칙 위반을 나타내는 공통 예외. 도메인 코드는 {@link ErrorCode} 구현으로 전달한다.
 * 공통 핸들러({@link GlobalExceptionHandler})가 code 로 매핑한다. 메시지 문자열로 분기하지 않는다.
 */
public class BusinessException extends RuntimeException {

    private final transient ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
