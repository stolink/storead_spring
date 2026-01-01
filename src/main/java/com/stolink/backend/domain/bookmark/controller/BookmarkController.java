package com.stolink.backend.domain.bookmark.controller;

import com.stolink.backend.domain.bookmark.dto.BookmarkResponse;
import com.stolink.backend.domain.bookmark.dto.ReadingProgressResponse;
import com.stolink.backend.domain.bookmark.dto.SaveBookmarkRequest;
import com.stolink.backend.domain.bookmark.service.BookmarkService;
import com.stolink.backend.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @GetMapping("/bookmarks/{chapterId}")
    public ApiResponse<BookmarkResponse> getBookmark(
            // @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID chapterId) {
        return bookmarkService.getBookmark(userId, chapterId)
                .map(ApiResponse::ok)
                .orElse(ApiResponse.ok(null));
    }

    @PostMapping("/bookmarks/{chapterId}")
    public ApiResponse<BookmarkResponse> saveBookmark(
            // @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID chapterId,
            @RequestBody SaveBookmarkRequest request) {
        BookmarkResponse bookmark = bookmarkService.saveBookmark(userId, chapterId, request);
        return ApiResponse.ok(bookmark);
    }

    @GetMapping("/works/{workId}/reading-progress")
    public ApiResponse<ReadingProgressResponse> getReadingProgress(
            // @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID workId) {
        ReadingProgressResponse progress = bookmarkService.getReadingProgress(userId, workId);
        return ApiResponse.ok(progress);
    }
}
