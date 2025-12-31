package com.stolink.backend.domain.rating.controller;

import com.stolink.backend.domain.rating.dto.RatingRequest;
import com.stolink.backend.domain.rating.dto.RatingResponse;
import com.stolink.backend.domain.rating.service.RatingService;
import com.stolink.backend.global.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping("/chapters/{id}/rating")
    public ApiResponse<RatingResponse> rateChapter(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody RatingRequest request) {
        RatingResponse response = ratingService.rateChapter(userId, id, request);
        return ApiResponse.ok(response);
    }

    @GetMapping("/chapters/{id}/rating")
    public ApiResponse<RatingResponse> getChapterRating(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id) {
        RatingResponse response = ratingService.getChapterRating(userId, id);
        return ApiResponse.ok(response);
    }

    @DeleteMapping("/chapters/{id}/rating")
    public ApiResponse<Void> deleteChapterRating(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id) {
        ratingService.deleteChapterRating(userId, id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/works/{id}/rating")
    public ApiResponse<RatingResponse> getWorkRating(@PathVariable UUID id) {
        RatingResponse response = ratingService.getWorkRating(id);
        return ApiResponse.ok(response);
    }
}
