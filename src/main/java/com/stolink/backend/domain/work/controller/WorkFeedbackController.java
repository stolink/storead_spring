package com.stolink.backend.domain.work.controller;

import com.stolink.backend.domain.work.dto.CreateFeedbackRequest;
import com.stolink.backend.domain.work.dto.WorkFeedbackResponse;
import com.stolink.backend.domain.work.service.WorkFeedbackService;
import com.stolink.backend.global.common.dto.ApiResponse;
import com.stolink.backend.global.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/discovery/works")
@RequiredArgsConstructor
public class WorkFeedbackController {

    private final WorkFeedbackService feedbackService;

    @GetMapping("/{workId}/feedback")
    public ApiResponse<WorkFeedbackResponse> getFeedback(
            @PathVariable UUID workId,
            @AuthenticationPrincipal Object principal) {
        UUID userId = SecurityUtils.extractUserId(principal);
        return ApiResponse.ok(feedbackService.getFeedbackCounts(workId, userId));
    }

    @PostMapping("/{workId}/feedback")
    public ApiResponse<Void> postFeedback(
            @PathVariable UUID workId,
            @RequestBody CreateFeedbackRequest request,
            @AuthenticationPrincipal Object principal) {
        UUID userId = SecurityUtils.extractUserId(principal);
        feedbackService.toggleFeedback(workId, userId, request);
        return ApiResponse.ok();
    }
}
