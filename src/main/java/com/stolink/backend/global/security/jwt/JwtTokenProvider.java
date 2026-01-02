package com.stolink.backend.global.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 토큰 생성 및 검증 Provider
 * stolink_spring과 동일한 secret을 사용하여 토큰 공유 (SSO)
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiry:1800000}")
    private long accessTokenExpiry; // 기본 30분

    @Value("${jwt.refresh-token-expiry:604800000}")
    private long refreshTokenExpiry; // 기본 7일

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT secret key must be at least 32 characters long");
        }
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Access Token 생성
     */
    public String createAccessToken(UUID userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiry);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("type", "access")
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Refresh Token 생성
     */
    public String createRefreshToken(UUID userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiry);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("type", "refresh")
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * 토큰에서 사용자 ID 추출
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Access Token 만료 시간(초) 반환
     */
    public long getAccessTokenExpirySeconds() {
        return accessTokenExpiry / 1000;
    }

    /**
     * Refresh Token 만료 시간(초) 반환
     */
    public long getRefreshTokenExpirySeconds() {
        return refreshTokenExpiry / 1000;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
