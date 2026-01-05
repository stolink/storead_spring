package com.stolink.backend.global.security.oauth2;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * OAuth2 인증 에러 코드
 * 
 * 클라이언트에 안전하게 노출 가능한 시스템 정의 에러 코드입니다.
 * 프론트엔드와 공유되는 에러 규격을 명확히 정의합니다.
 */
@Getter
@RequiredArgsConstructor
public enum OAuth2ErrorCode {

    /** 일반적인 인증 실패 */
    AUTH_FAILED("AUTH_FAILED", "인증에 실패했습니다."),

    /** 유효하지 않은 토큰 */
    INVALID_TOKEN("INVALID_TOKEN", "유효하지 않은 토큰입니다."),

    /** 접근 거부 */
    ACCESS_DENIED("ACCESS_DENIED", "접근이 거부되었습니다."),

    /** 서버 오류 */
    SERVER_ERROR("SERVER_ERROR", "서버 오류가 발생했습니다.");

    private final String code;
    private final String message;

    /**
     * OAuth2 에러 코드를 시스템 에러 코드로 매핑
     * 내부 에러 정보를 숨기고 안전한 에러 코드만 반환
     *
     * @param oauthErrorCode OAuth2 제공자로부터 받은 에러 코드
     * @return 시스템 정의 에러 코드
     */
    public static OAuth2ErrorCode fromOAuthErrorCode(String oauthErrorCode) {
        if (oauthErrorCode == null) {
            return AUTH_FAILED;
        }

        return switch (oauthErrorCode.toLowerCase()) {
            case "invalid_token", "invalid_grant", "invalid_request" -> INVALID_TOKEN;
            case "access_denied", "unauthorized_client" -> ACCESS_DENIED;
            case "server_error", "temporarily_unavailable" -> SERVER_ERROR;
            default -> AUTH_FAILED;
        };
    }
}
