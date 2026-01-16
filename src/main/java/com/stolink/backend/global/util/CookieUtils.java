package com.stolink.backend.global.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * JWT 토큰 쿠키 생성 유틸리티
 * HttpOnly, Secure, SameSite 설정으로 XSS/CSRF 방어
 */
@Component
public class CookieUtils {

    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    @Value("${jwt.cookie-domain:localhost}")
    private String cookieDomain;

    @Value("${jwt.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${jwt.access-token-expiry:1800000}")
    private long accessTokenExpiry;

    @Value("${jwt.refresh-token-expiry:604800000}")
    private long refreshTokenExpiry;

    @Value("${jwt.cookie-same-site:Lax}")
    private String cookieSameSite;

    /**
     * Access Token 쿠키 생성
     * Path: / (모든 경로에서 전송)
     */
    public ResponseCookie createAccessTokenCookie(String accessToken) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(ACCESS_TOKEN_COOKIE, accessToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(accessTokenExpiry / 1000)
                .sameSite(cookieSameSite);

        if (cookieDomain != null && !cookieDomain.equals("localhost")) {
            builder.domain(cookieDomain);
        }

        return builder.build();
    }

    /**
     * Refresh Token 쿠키 생성
     * Path: / (모든 경로에서 전송)
     */
    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(refreshTokenExpiry / 1000)
                .sameSite(cookieSameSite);

        if (cookieDomain != null && !cookieDomain.equals("localhost")) {
            builder.domain(cookieDomain);
        }

        return builder.build();
    }

    /**
     * Access Token 쿠키 만료 (로그아웃용)
     */
    public ResponseCookie createExpiredAccessTokenCookie() {
        return createExpiredCookie(ACCESS_TOKEN_COOKIE, "/");
    }

    /**
     * Refresh Token 쿠키 만료 (로그아웃용)
     */
    public ResponseCookie createExpiredRefreshTokenCookie() {
        return createExpiredCookie(REFRESH_TOKEN_COOKIE, "/");
    }

    private ResponseCookie createExpiredCookie(String name, String path) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path(path)
                .maxAge(0)
                .sameSite(cookieSameSite);

        if (cookieDomain != null && !cookieDomain.equals("localhost")) {
            builder.domain(cookieDomain);
        }

        return builder.build();
    }
}
