package com.stolink.backend.domain.stats.controller;

import com.stolink.backend.domain.stats.service.AuthorStatsService;
import com.stolink.backend.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/author/works/{workId}")
@RequiredArgsConstructor
public class AuthorStatsController {

    private final AuthorStatsService authorStatsService;

    // 통계 권한 체크: 본인 작품인지 확인 필요 (MVP에서는 생략 or 추후 추가)
    // @PreAuthorize("@workSecurity.isOwner(#workId, authentication.principal)")

    @GetMapping("/stats")
    public ApiResponse<AuthorStatsService.WorkStatsDto> getWorkStats(@PathVariable UUID workId) {
        return ApiResponse.ok(authorStatsService.getWorkStats(workId));
    }

    @GetMapping("/retention")
    public ApiResponse<List<AuthorStatsService.RetentionDto>> getRetentionRates(@PathVariable UUID workId) {
        return ApiResponse.ok(authorStatsService.getRetentionRates(workId));
    }

    @GetMapping("/discovery")
    public ApiResponse<List<AuthorStatsService.DiscoveryDto>> getDiscoverySources(@PathVariable UUID workId) {
        return ApiResponse.ok(authorStatsService.getDiscoverySources(workId));
    }
}
