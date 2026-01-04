package com.stolink.backend.domain.user.controller;

import com.stolink.backend.domain.user.dto.*;
import com.stolink.backend.domain.user.service.AuthService;
import com.stolink.backend.global.common.dto.ApiResponse;
import com.stolink.backend.global.util.CookieUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${jwt.cookie-domain}")
    private String cookieDomain;

    @Value("${jwt.cookie-secure:true}")
    private boolean cookieSecure;

    @Value("${jwt.access-token-expiry:1800000}")
    private long accessTokenExpiry;

    /**
     * 일반 회원가입
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody RegisterRequest request) {
        TokenResponse token = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, createAccessTokenCookie(token.getAccessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(token.getRefreshToken()).toString())
                .body(ApiResponse.created(AuthResponse.from(token)));
    }

    /**
     * 일반 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest request) {
        TokenResponse token = authService.login(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createAccessTokenCookie(token.getAccessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(token.getRefreshToken()).toString())
                .body(ApiResponse.ok(AuthResponse.from(token)));
    }

    /**
     * 토큰 갱신 (Cookie 사용)
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(value = "refresh_token", required = false) String refreshToken) {
        if (refreshToken == null) {
            throw new IllegalArgumentException("Refresh Token이 쿠키에 없습니다.");
        }

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        TokenResponse token = authService.refreshToken(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createAccessTokenCookie(token.getAccessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(token.getRefreshToken()).toString())
                .body(ApiResponse.ok(AuthResponse.from(token)));
    }

    /**
     * 로그아웃 (현재 디바이스)
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(value = "refresh_token", required = false) String refreshToken) {
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        ResponseCookie accessCookie = CookieUtils.deleteCookie("access_token", cookieDomain, cookieSecure);
        ResponseCookie refreshCookie = CookieUtils.deleteCookie("refresh_token", cookieDomain, cookieSecure);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.ok(null));
    }

    /**
     * 전체 로그아웃 (모든 디바이스에서 로그아웃)
     */
    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(@AuthenticationPrincipal UUID userId) {
        authService.logoutAll(userId);

        ResponseCookie accessCookie = CookieUtils.deleteCookie("access_token", cookieDomain, cookieSecure);
        ResponseCookie refreshCookie = CookieUtils.deleteCookie("refresh_token", cookieDomain, cookieSecure);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.ok(null));
    }

    /**
     * 현재 사용자 정보 조회 (JWT 인증 필요)
     */
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMe(@AuthenticationPrincipal UUID userId) {
        UserResponse user = authService.getUser(userId);
        return ApiResponse.ok(user);
    }

    /**
     * 사용자 프로필 업데이트 (JWT 인증 필요)
     */
    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateMe(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String avatarUrl) {
        UserResponse user = authService.updateUser(userId, nickname, avatarUrl);
        return ApiResponse.ok(user);
    }

    private ResponseCookie createAccessTokenCookie(String accessToken) {
        return CookieUtils.createCookie(
                "access_token",
                accessToken,
                cookieDomain,
                cookieSecure,
                accessTokenExpiry / 1000 // ms -> seconds
        );
    }

    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return CookieUtils.createCookie(
                "refresh_token",
                refreshToken,
                cookieDomain,
                cookieSecure,
                7 * 24 * 60 * 60 // 7 days
        );
    }
}
