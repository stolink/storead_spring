package com.stolink.backend.global.security.oauth2;

import com.stolink.backend.domain.user.service.AuthService;
import com.stolink.backend.global.security.jwt.JwtTokenProvider;
import com.stolink.backend.global.util.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * OAuth2 로그인 성공 핸들러
 *
 * OAuth2 인증 성공 후 JWT 토큰을 쿠키로 발급하고
 * 프론트엔드로 리다이렉트합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

        private final JwtTokenProvider jwtTokenProvider;
        private final AuthService authService;

        @Value("${oauth2.redirect-uri:http://localhost:5174/oauth2/callback}")
        private String redirectUri;

        @Value("${jwt.cookie-domain}")
        private String cookieDomain;

        @Value("${jwt.cookie-secure:true}")
        private boolean cookieSecure;

        @Override
        public void onAuthenticationSuccess(HttpServletRequest request,
                        HttpServletResponse response,
                        Authentication authentication) throws IOException {
                OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

                // CustomOAuth2UserService에서 설정한 userId 추출
                String userIdStr = (String) oAuth2User.getAttributes().get("userId");
                UUID userId = UUID.fromString(userIdStr);

                // JWT 토큰 생성
                String accessToken = jwtTokenProvider.createAccessToken(userId);
                String refreshToken = jwtTokenProvider.createRefreshToken(userId);

                log.info("OAuth2 login success. Issuing JWT cookies for user: {}", userId);

                // Refresh Token을 RDB에 저장
                authService.saveRefreshToken(userId, refreshToken);

                // Access Token을 HttpOnly 쿠키로 설정
                ResponseCookie accessCookie = CookieUtils.createCookie(
                                "access_token",
                                accessToken,
                                cookieDomain,
                                cookieSecure,
                                jwtTokenProvider.getAccessTokenExpirySeconds());
                response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

                // Refresh Token을 HttpOnly 쿠키로 설정
                ResponseCookie refreshCookie = CookieUtils.createCookie(
                                "refresh_token",
                                refreshToken,
                                cookieDomain,
                                cookieSecure,
                                7 * 24 * 60 * 60 // 7일
                );
                response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

                // 프론트엔드로 리다이렉트 (토큰 없이, 쿠키로 전달)
                getRedirectStrategy().sendRedirect(request, response, redirectUri);
        }
}
