package com.stolink.backend.domain.chapter.controller;

import com.stolink.backend.domain.chapter.dto.*;
import com.stolink.backend.domain.chapter.service.ChapterService;
import com.stolink.backend.global.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChapterController {

    private final ChapterService chapterService;

    @GetMapping("/works/{workId}/chapters")
    public ApiResponse<List<ChapterResponse>> getChapters(
            // @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID workId) {
        List<ChapterResponse> chapters = chapterService.getChapters(userId, workId);
        return ApiResponse.ok(chapters);
    }

    @PostMapping("/works/{workId}/chapters")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChapterResponse> createChapter(
            // @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID workId,
            @Valid @RequestBody CreateChapterRequest request) {
        ChapterResponse chapter = chapterService.createChapter(userId, workId, request);
        return ApiResponse.created(chapter);
    }

    @GetMapping("/chapters/{id}")
    public ApiResponse<ChapterDetailResponse> getChapter(
            // @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID id) {
        ChapterDetailResponse chapter = chapterService.getChapter(userId, id);
        return ApiResponse.ok(chapter);
    }

    @PatchMapping("/chapters/{id}")
    public ApiResponse<ChapterDetailResponse> updateChapter(
            // @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID id,
            @RequestBody UpdateChapterRequest request) {
        ChapterDetailResponse chapter = chapterService.updateChapter(userId, id, request);
        return ApiResponse.ok(chapter);
    }

    @DeleteMapping("/chapters/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteChapter(
            // @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID id) {
        chapterService.deleteChapter(userId, id);
    }
}
