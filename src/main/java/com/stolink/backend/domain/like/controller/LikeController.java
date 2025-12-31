package com.stolink.backend.domain.like.controller;

import com.stolink.backend.domain.like.dto.LikeResponse;
import com.stolink.backend.domain.like.service.LikeService;
import com.stolink.backend.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/{id}/like")
    public ApiResponse<LikeResponse> toggleCommentLike(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id) {
        LikeResponse response = likeService.toggleCommentLike(userId, id);
        return ApiResponse.ok(response);
    }
}
