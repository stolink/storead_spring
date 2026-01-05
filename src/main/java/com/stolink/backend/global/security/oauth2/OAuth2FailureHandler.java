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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAuth2 로그인 실패 핸들러
 *
 * OAuth2 인증 실패 시 예외를 로깅하고
 * 프론트엔드로 에러 정보와 함께 리다이렉트합니다.
 */
@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${oauth2.redirect-uri:http://localhost:5174/oauth2/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        // 1. 상세 에러 로깅
        log.error("=== OAuth2 Authentication Failed ===");
        log.error("Exception Type: {}", exception.getClass().getSimpleName());
        log.error("Error Message: {}", exception.getMessage());

        if (exception instanceof OAuth2AuthenticationException oauthException) {
            log.error("OAuth2 Error Code: {}", oauthException.getError().getErrorCode());
            log.error("OAuth2 Error Description: {}", oauthException.getError().getDescription());
            log.error("OAuth2 Error URI: {}", oauthException.getError().getUri());
        }

        // Request 정보 로깅 (디버깅용)
        log.error("Request URI: {}", request.getRequestURI());
        log.error("Query String: {}", request.getQueryString());

        if (exception.getCause() != null) {
            log.error("Root Cause: {}", exception.getCause().getMessage(), exception.getCause());
        }

        log.error("Full Stack Trace:", exception);
        log.error("=================================");

        // 2. 프론트엔드로 에러 정보와 함께 리다이렉트
        String errorMessage = exception.getMessage() != null
                ? exception.getMessage()
                : "OAuth2 인증에 실패했습니다.";

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("error", URLEncoder.encode(errorMessage, StandardCharsets.UTF_8))
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
