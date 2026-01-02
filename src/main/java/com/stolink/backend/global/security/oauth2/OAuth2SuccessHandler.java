package com.stolink.backend.global.security.oauth2;

import com.stolink.backend.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.UUID;

/**
 * OAuth2 로그인 성공 핸들러
 *
 * OAuth2 인증 성공 후 JWT 토큰을 발급하고
 * 프론트엔드로 리다이렉트합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${oauth2.redirect-uri:http://localhost:5174/oauth2/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // CustomOAuth2UserService에서 설정한 userId 추출
        String userIdStr = (String) oAuth2User.getAttributes().get("userId");
        UUID userId = UUID.fromString(userIdStr);

        // JWT Access Token 생성
        String accessToken = jwtTokenProvider.createAccessToken(userId);

        log.info("OAuth2 login success. Issuing JWT for user: {}", userId);

        // 프론트엔드로 리다이렉트 (Query Param으로 토큰 전달)
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", accessToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
