package com.stolink.backend.domain.comment.controller;

import com.stolink.backend.domain.comment.dto.CommentResponse;
import com.stolink.backend.domain.comment.dto.CreateCommentRequest;
import com.stolink.backend.domain.comment.service.CommentService;
import com.stolink.backend.global.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/chapters/{chapterId}/comments")
    public ApiResponse<Map<String, Object>> getComments(
            @PathVariable UUID chapterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<CommentResponse> comments = commentService.getComments(chapterId, pageable);

        return ApiResponse.ok(Map.of(
                "comments", comments.getContent(),
                "pagination", Map.of(
                        "page", page,
                        "size", size,
                        "total", comments.getTotalElements(),
                        "totalPages", comments.getTotalPages(),
                        "hasNext", comments.hasNext()
                )
        ));
    }

    @PostMapping("/chapters/{chapterId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentResponse> createComment(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID chapterId,
            @Valid @RequestBody CreateCommentRequest request) {
        CommentResponse comment = commentService.createComment(userId, chapterId, request);
        return ApiResponse.created(comment);
    }

    @GetMapping("/comments/{id}/replies")
    public ApiResponse<List<CommentResponse>> getReplies(@PathVariable UUID id) {
        List<CommentResponse> replies = commentService.getReplies(id);
        return ApiResponse.ok(replies);
    }

    @PostMapping("/comments/{id}/replies")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentResponse> createReply(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody CreateCommentRequest request) {
        CommentResponse reply = commentService.createReply(userId, id, request);
        return ApiResponse.created(reply);
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        commentService.deleteComment(userId, id);
    }
}
