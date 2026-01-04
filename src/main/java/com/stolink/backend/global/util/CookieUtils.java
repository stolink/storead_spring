package com.stolink.backend.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Optional;

/**
 * 쿠키 관련 유틸리티 클래스
 */
public class CookieUtils {

    /**
     * 요청에서 특정 이름의 쿠키 값을 가져옵니다.
     */
    public static Optional<String> getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals(name))
                .map(Cookie::getValue)
                .filter(StringUtils::hasText)
                .findFirst();
    }

    /**
     * 요청에서 특정 이름의 쿠키를 가져옵니다.
     */
    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals(name))
                .findFirst();
    }

    /**
     * 쿠키를 생성합니다.
     */
    public static ResponseCookie createCookie(String name, String value, String domain,
            boolean secure, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .domain(domain)
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")
                .build();
    }

    /**
     * 쿠키를 삭제합니다 (maxAge=0으로 설정).
     */
    public static ResponseCookie deleteCookie(String name, String domain, boolean secure) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .domain(domain)
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }
}
