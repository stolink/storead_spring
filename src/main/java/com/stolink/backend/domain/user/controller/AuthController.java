package com.stolink.backend.domain.user.controller;

import com.stolink.backend.domain.user.dto.LoginRequest;
import com.stolink.backend.domain.user.dto.RegisterRequest;
import com.stolink.backend.domain.user.dto.TokenResponse;
import com.stolink.backend.domain.user.dto.UserResponse;
import com.stolink.backend.domain.user.service.AuthService;
import com.stolink.backend.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TokenResponse> register(@RequestBody RegisterRequest request) {
        TokenResponse token = authService.register(request);
        return ApiResponse.created(token);
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@RequestBody LoginRequest request) {
        TokenResponse token = authService.login(request);
        return ApiResponse.ok(token);
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMe(@AuthenticationPrincipal UUID userId) {
        UserResponse user = authService.getUser(userId);
        return ApiResponse.ok(user);
    }

    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateMe(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String avatarUrl) {
        UserResponse user = authService.updateUser(userId, nickname, avatarUrl);
        return ApiResponse.ok(user);
    }
}
