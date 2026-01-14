package com.stolink.backend.domain.user.controller;

import com.stolink.backend.domain.user.dto.*;
import com.stolink.backend.domain.user.service.AuthService;
import com.stolink.backend.global.common.dto.ApiResponse;
import com.stolink.backend.global.util.CookieUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final CookieUtils cookieUtils;



    /**
     * 일반 회원가입
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody RegisterRequest request) {
        TokenResponse token = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookieUtils.createAccessTokenCookie(token.getAccessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtils.createRefreshTokenCookie(token.getRefreshToken()).toString())
                .body(ApiResponse.created(AuthResponse.from(token)));
    }

    /**
     * 일반 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest request) {
        TokenResponse token = authService.login(request);

        ResponseCookie accessCookie = cookieUtils.createAccessTokenCookie(token.getAccessToken());
        ResponseCookie refreshCookie = cookieUtils.createRefreshTokenCookie(token.getRefreshToken());

        log.debug("=== Login Success ===");
        log.debug("Access Token Cookie created");
        log.debug("Refresh Token Cookie created");

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
        headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok()
                .headers(consumer -> consumer.addAll(headers))
                .body(ApiResponse.ok(AuthResponse.from(token)));
    }

    /**
     * 토큰 갱신 (Cookie 사용)
     */
    @PostMapping("/refresh")

    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(value = "refresh_token", required = false) String refreshToken) {
        log.debug("=== Refresh Token Request ===");
        
        try {
            if (refreshToken == null) {
                log.warn("Refresh Token is missing in cookie");
                throw new IllegalArgumentException("Refresh Token이 쿠키에 없습니다.");
            }

            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken(refreshToken);

            log.debug("Calling authService.refreshToken");
            TokenResponse token = authService.refreshToken(request);
            log.info("Token refreshed successfully");

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookieUtils.createAccessTokenCookie(token.getAccessToken()).toString())
                    .header(HttpHeaders.SET_COOKIE, cookieUtils.createRefreshTokenCookie(token.getRefreshToken()).toString())
                    .body(ApiResponse.ok(AuthResponse.from(token)));
        } catch (Exception e) {
            log.error("Exception in refresh token process: ", e);
            throw e;
        }
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

        ResponseCookie accessCookie = cookieUtils.createExpiredAccessTokenCookie();
        ResponseCookie refreshCookie = cookieUtils.createExpiredRefreshTokenCookie();

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

        ResponseCookie accessCookie = cookieUtils.createExpiredAccessTokenCookie();
        ResponseCookie refreshCookie = cookieUtils.createExpiredRefreshTokenCookie();

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


}
