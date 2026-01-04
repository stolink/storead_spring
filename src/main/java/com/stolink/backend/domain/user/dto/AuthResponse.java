package com.stolink.backend.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 인증 응답 DTO (쿠키 기반 - 토큰은 쿠키로 전달)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private Long expiresIn; // seconds
    private UserResponse user;

    public static AuthResponse from(TokenResponse tokenResponse) {
        return AuthResponse.builder()
                .expiresIn(tokenResponse.getExpiresIn())
                .user(tokenResponse.getUser())
                .build();
    }
}
