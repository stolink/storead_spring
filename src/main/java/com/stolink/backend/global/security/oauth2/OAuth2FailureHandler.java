package com.stolink.backend.global.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * OAuth2 로그인 실패 핸들러
 *
 * OAuth2 인증 실패 시 예외를 로깅하고
 * 프론트엔드로 에러 코드와 함께 리다이렉트합니다.
 * 
 * 보안 고려사항:
 * - 내부 예외 메시지를 클라이언트에 노출하지 않음
 * - 시스템 정의 에러 코드만 전달 (OAuth2ErrorCode Enum)
 * - Redirect URI 화이트리스트 검증으로 Open Redirect 방지
 * - 호스트 비교 시 대소문자 구분하지 않음
 */
@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${oauth2.redirect-uri}")
    private String redirectUri;

    @Value("${oauth2.default-failure-url:http://localhost:5174/oauth2/callback}")
    private String defaultFailureUrl;

    // 허용된 리다이렉트 도메인 화이트리스트 (환경별 필수 설정 필요)
    @Value("${oauth2.allowed-redirect-hosts}")
    private List<String> allowedRedirectHosts;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        // 1. 상세 에러 로깅 (서버 로그에만 기록, 클라이언트에는 노출되지 않음)
        log.error("=== OAuth2 Authentication Failed ===");
        log.error("Exception Type: {}", exception.getClass().getSimpleName());
        log.error("Error Message: {}", exception.getMessage());

        OAuth2ErrorCode errorCode = OAuth2ErrorCode.AUTH_FAILED;

        if (exception instanceof OAuth2AuthenticationException oauthException) {
            String oauthErrorCode = oauthException.getError().getErrorCode();
            log.error("OAuth2 Error Code: {}", oauthErrorCode);
            log.error("OAuth2 Error Description: {}", oauthException.getError().getDescription());
            log.error("OAuth2 Error URI: {}", oauthException.getError().getUri());

            // OAuth2 에러 코드를 시스템 에러 코드로 매핑
            errorCode = OAuth2ErrorCode.fromOAuthErrorCode(oauthErrorCode);
        }

        // Request 정보 로깅 (디버깅용)
        log.error("Request URI: {}", request.getRequestURI());
        log.error("Query String: {}", request.getQueryString());

        if (exception.getCause() != null) {
            log.error("Root Cause: {}", exception.getCause().getMessage(), exception.getCause());
        }

        log.error("Full Stack Trace:", exception);
        log.error("=================================");

        // 2. Redirect URI 검증 (Open Redirect 취약점 방지)
        String validatedRedirectUri = validateAndGetRedirectUri();

        // 3. 프론트엔드로 시스템 에러 코드와 함께 리다이렉트
        // encode()를 명시적으로 호출하여 특수 문자 처리 안정성 확보
        String targetUrl = UriComponentsBuilder.fromUriString(validatedRedirectUri)
                .queryParam("error", errorCode.getCode())
                .encode()
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /**
     * Redirect URI 검증 및 반환
     * 허용된 호스트 목록에 포함된 경우에만 해당 URI 반환
     * 호스트 비교 시 대소문자를 구분하지 않음 (RFC 3986)
     * 검증 실패 시 설정된 기본 실패 URL 반환
     */
    private String validateAndGetRedirectUri() {
        try {
            URI uri = URI.create(redirectUri);
            String host = uri.getHost();

            // 대소문자 구분 없이 호스트 비교 (HTTP 호스트명은 대소문자 구분하지 않음)
            if (host != null && allowedRedirectHosts.stream()
                    .anyMatch(allowedHost -> allowedHost.equalsIgnoreCase(host))) {
                return redirectUri;
            }

            log.warn("Redirect URI host '{}' is not in allowed list {}. Using default failure URL.",
                    host, allowedRedirectHosts);
        } catch (IllegalArgumentException e) {
            log.error("Invalid redirect URI format: {}", redirectUri, e);
        }

        // 검증 실패 시 설정된 기본 실패 URL 반환
        return defaultFailureUrl;
    }
}
