package com.stolink.backend.domain.like.controller;

import com.stolink.backend.domain.like.dto.LikeResponse;
import com.stolink.backend.domain.like.service.LikeService;
import com.stolink.backend.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/api/comments/{id}/like")
    public ApiResponse<LikeResponse> toggleCommentLike(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        LikeResponse response = likeService.toggleCommentLike(userId, id);
        return ApiResponse.ok(response);
    }

    @PostMapping("/api/works/{id}/like")
    public ApiResponse<LikeResponse> toggleWorkLike(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        LikeResponse response = likeService.toggleWorkLike(userId, id);
        return ApiResponse.ok(response);
    }

    @GetMapping("/api/works/{id}/like")
    public ApiResponse<LikeResponse> getWorkLikeStatus(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        LikeResponse response = likeService.getWorkLikeStatus(userId, id);
        return ApiResponse.ok(response);
    }
}
