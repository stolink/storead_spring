package com.stolink.backend.global.common.exception;

/**
 * 401 Unauthorized 예외
 * - 로그인이 필요한 API에 비로그인 상태로 접근할 때 발생
 * - X-User-Id 헤더가 없거나 null일 때 발생
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
