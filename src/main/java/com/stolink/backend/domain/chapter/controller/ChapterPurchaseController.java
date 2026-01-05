package com.stolink.backend.domain.chapter.controller;

import com.stolink.backend.domain.chapter.dto.PurchaseCheckResponse;
import com.stolink.backend.domain.chapter.service.ChapterPurchaseService;
import com.stolink.backend.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chapters")
@RequiredArgsConstructor
public class ChapterPurchaseController {

    private final ChapterPurchaseService chapterPurchaseService;

    /**
     * Helper to extract UUID from principal
     * - UUID 직접 전달 시 처리
     * - UserDetails 구현체에서 UUID 추출 (security 설정에 따라)
     */
    private UUID extractUserId(Object principal) {
        if (principal == null) {
            return null;
        }
        if (principal instanceof UUID) {
            return (UUID) principal;
        }
        // Spring Security UserDetails 구현체 처리
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            try {
                return UUID.fromString(username);
            } catch (IllegalArgumentException e) {
                // username이 UUID 형식이 아닌 경우
                return null;
            }
        }
        return null;
    }

    /**
     * 챕터 구매 가능 여부 확인
     */
    @GetMapping("/{id}/purchase/check")
    public ApiResponse<PurchaseCheckResponse> checkPurchaseAvailability(
            @PathVariable UUID id,
            @AuthenticationPrincipal Object principal) {

        UUID userId = extractUserId(principal);
        if (userId == null) {
            // 비로그인 시 일단 기본 응답 (혹은 401 에러)
            // 여기서는 조회용 API이므로 에러 대신 구매 불가 응답 가능
            // 하지만 프론트엔드에서 로그인 체크하므로 에러 던지는게 나을수도.
            // 기존 컨트롤러 패턴 따름.
            throw new IllegalArgumentException("User not authenticated");
        }

        PurchaseCheckResponse response = chapterPurchaseService.checkPurchaseAvailability(userId, id);
        return ApiResponse.ok(response);
    }

    /**
     * 챕터 구매
     */
    @PostMapping("/{id}/purchase")
    public ApiResponse<Map<String, Object>> purchaseChapter(
            @PathVariable UUID id,
            @AuthenticationPrincipal Object principal) {

        UUID userId = extractUserId(principal);
        if (userId == null) {
            throw new IllegalArgumentException("User not authenticated");
        }

        chapterPurchaseService.purchaseChapter(userId, id);
        return ApiResponse.ok(Map.of("success", true, "chapterId", id));
    }

    /**
     * 챕터 접근 권한 확인
     */
    @GetMapping("/{id}/access")
    public ApiResponse<Map<String, Object>> checkAccess(
            @PathVariable UUID id,
            @AuthenticationPrincipal Object principal) {

        UUID userId = extractUserId(principal);
        if (userId == null) {
            // 비로그인
            return ApiResponse.ok(Map.of("hasAccess", false));
        }

        boolean hasAccess = chapterPurchaseService.hasAccess(userId, id);
        return ApiResponse.ok(Map.of("hasAccess", hasAccess));
    }
}
