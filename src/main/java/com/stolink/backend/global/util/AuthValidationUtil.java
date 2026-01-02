package com.stolink.backend.global.util;

import com.stolink.backend.global.common.exception.UnauthorizedException;

import java.util.UUID;

/**
 * 인증 관련 유틸리티 클래스
 * - 각 Service 레이어에서 userId null 체크에 사용
 */
public class AuthValidationUtil {

    /**
     * userId가 null인지 검증하고, null이면 UnauthorizedException 발생
     * 
     * @param userId 검증할 사용자 ID
     * @throws UnauthorizedException userId가 null인 경우
     */
    public static void requireUserId(UUID userId) {
        if (userId == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
    }

    /**
     * userId가 null인지 검증하고, null이면 커스텀 메시지로 UnauthorizedException 발생
     * 
     * @param userId  검증할 사용자 ID
     * @param message 예외 메시지
     * @throws UnauthorizedException userId가 null인 경우
     */
    public static void requireUserId(UUID userId, String message) {
        if (userId == null) {
            throw new UnauthorizedException(message);
        }
    }
}
