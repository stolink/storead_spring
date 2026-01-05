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
 * - 시스템 정의 에러 코드만 전달
 * - Redirect URI 화이트리스트 검증으로 Open Redirect 방지
 */
@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    // 시스템 정의 에러 코드 (클라이언트에 안전하게 노출 가능)
    private static final String ERROR_CODE_AUTH_FAILED = "AUTH_FAILED";
    private static final String ERROR_CODE_INVALID_TOKEN = "INVALID_TOKEN";
    private static final String ERROR_CODE_ACCESS_DENIED = "ACCESS_DENIED";
    private static final String ERROR_CODE_SERVER_ERROR = "SERVER_ERROR";

    @Value("${oauth2.redirect-uri:http://localhost:5174/oauth2/callback}")
    private String redirectUri;

    // 허용된 리다이렉트 도메인 화이트리스트
    @Value("${oauth2.allowed-redirect-hosts:localhost}")
    private List<String> allowedRedirectHosts;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        // 1. 상세 에러 로깅 (서버 로그에만 기록, 클라이언트에는 노출되지 않음)
        log.error("=== OAuth2 Authentication Failed ===");
        log.error("Exception Type: {}", exception.getClass().getSimpleName());
        log.error("Error Message: {}", exception.getMessage());

        String errorCode = ERROR_CODE_AUTH_FAILED;

        if (exception instanceof OAuth2AuthenticationException oauthException) {
            String oauthErrorCode = oauthException.getError().getErrorCode();
            log.error("OAuth2 Error Code: {}", oauthErrorCode);
            log.error("OAuth2 Error Description: {}", oauthException.getError().getDescription());
            log.error("OAuth2 Error URI: {}", oauthException.getError().getUri());

            // OAuth2 에러 코드를 시스템 에러 코드로 매핑
            errorCode = mapToSystemErrorCode(oauthErrorCode);
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
        // UriComponentsBuilder가 자동으로 RFC 3986 인코딩을 수행함
        String targetUrl = UriComponentsBuilder.fromUriString(validatedRedirectUri)
                .queryParam("error", errorCode)
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /**
     * OAuth2 에러 코드를 시스템 정의 에러 코드로 매핑
     * 내부 에러 정보를 숨기고 안전한 에러 코드만 반환
     */
    private String mapToSystemErrorCode(String oauthErrorCode) {
        if (oauthErrorCode == null) {
            return ERROR_CODE_AUTH_FAILED;
        }

        return switch (oauthErrorCode.toLowerCase()) {
            case "invalid_token", "invalid_grant", "invalid_request" -> ERROR_CODE_INVALID_TOKEN;
            case "access_denied", "unauthorized_client" -> ERROR_CODE_ACCESS_DENIED;
            case "server_error", "temporarily_unavailable" -> ERROR_CODE_SERVER_ERROR;
            default -> ERROR_CODE_AUTH_FAILED;
        };
    }

    /**
     * Redirect URI 검증 및 반환
     * 허용된 호스트 목록에 포함된 경우에만 해당 URI 반환
     * 검증 실패 시 기본 리다이렉트 URI 반환
     */
    private String validateAndGetRedirectUri() {
        try {
            URI uri = URI.create(redirectUri);
            String host = uri.getHost();

            if (host != null && allowedRedirectHosts.contains(host)) {
                return redirectUri;
            }

            log.warn("Redirect URI host '{}' is not in allowed list. Using default.", host);
        } catch (IllegalArgumentException e) {
            log.error("Invalid redirect URI format: {}", redirectUri, e);
        }

        // 검증 실패 시 안전한 기본값 반환
        return "http://localhost:5174/oauth2/callback";
    }
}
