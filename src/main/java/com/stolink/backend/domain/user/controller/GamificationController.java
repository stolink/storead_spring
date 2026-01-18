package com.stolink.backend.domain.user.controller;

import com.stolink.backend.domain.user.dto.AttendanceResponse;
import com.stolink.backend.domain.user.dto.GamificationResponse;
import com.stolink.backend.domain.user.service.GamificationService;
import com.stolink.backend.global.common.dto.ApiResponse;
import com.stolink.backend.global.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;

    @GetMapping("/gamification")
    public ApiResponse<GamificationResponse> getMyGamification(@AuthenticationPrincipal Object principal) {
        UUID userId = SecurityUtils.extractUserId(principal);
        return ApiResponse.ok(gamificationService.getMyGamification(userId));
    }

    @PostMapping("/attendance")
    public ApiResponse<AttendanceResponse> checkAttendance(@AuthenticationPrincipal Object principal) {
        UUID userId = SecurityUtils.extractUserId(principal);
        return ApiResponse.ok(gamificationService.checkAttendance(userId));
    }
}
