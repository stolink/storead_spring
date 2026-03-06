package com.stolink.backend.global.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

/**
 * JWT 인증 필터
 * 쿠키 또는 Authorization 헤더에서 JWT 토큰을 추출하여 검증
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ACCESS_TOKEN_COOKIE = "access_token";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = resolveToken(request);

            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                UUID userId = jwtTokenProvider.getUserIdFromToken(jwt);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId, null, Collections.emptyList());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Set Authentication to SecurityContext for user: {}", userId);
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 토큰 추출: 쿠키 우선, 없으면 Authorization 헤더에서 추출
     */
    private String resolveToken(HttpServletRequest request) {
        // 1. 쿠키에서 확인 (유효한 토큰 탐색)
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        String token = null;

        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                    token = cookie.getValue();
                    log.debug("Found access_token cookie from request.getCookies(): {}...",
                            token.substring(0, Math.min(token.length(), 10)));
                    break;
                }
            }
        }

        // 1-1. request.getCookies()가 null인 경우 Header에서 직접 추출 (일부 프록시/멀티파트 환경 대응)
        if (token == null) {
            String cookieHeader = request.getHeader("Cookie");
            if (StringUtils.hasText(cookieHeader)) {
                token = parseCookie(cookieHeader, ACCESS_TOKEN_COOKIE);
                if (token != null) {
                    log.debug("Found access_token cookie from Header: {}...",
                            token.substring(0, Math.min(token.length(), 10)));
                }
            }
        }

        if (token != null) {
            try {
                if (jwtTokenProvider.validateToken(token)) {
                    return token;
                } else {
                    log.debug("Token validation failed for cookie");
                }
            } catch (Exception e) {
                log.debug("Token validation exception: {}", e.getMessage());
            }
        } else {
            log.debug("No cookies found in request");
        }

        // 2. Authorization 헤더에서 확인 (하위 호환성)
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            log.debug("Found Bearer token in header");
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        log.debug("No valid token resolved (Anonymous)");
        return null;
    }

    /**
     * Cookie 헤더 문자열에서 특정 쿠키 값을 추출
     */
    private String parseCookie(String cookieHeader, String cookieName) {
        String[] cookies = cookieHeader.split(";");
        for (String cookie : cookies) {
            String[] pair = cookie.trim().split("=");
            if (pair.length == 2 && pair[0].equals(cookieName)) {
                return pair[1];
            }
        }
        return null;
    }
}
