package com.stolink.backend.domain.work.controller;

import com.stolink.backend.domain.work.dto.CreateWorkRequest;
import com.stolink.backend.domain.work.dto.UpdateWorkRequest;
import com.stolink.backend.domain.work.dto.WorkResponse;
import com.stolink.backend.domain.work.service.WorkService;
import com.stolink.backend.global.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/works")
@RequiredArgsConstructor
public class WorkController {

    private final WorkService workService;

    /**
     * projectId로 작품 조회 (커뮤니티 배포용)
     * GET /api/works?projectId={projectId}
     * 
     * 프론트엔드에서 기존 작품이 있는지 확인할 때 사용
     */
    @GetMapping(params = "projectId")
    public ApiResponse<Map<String, Object>> getWorkByProjectId(
            @RequestParam String projectId) {

        Optional<WorkResponse> work = workService.findByProjectId(projectId);

        Map<String, Object> response = new HashMap<>();
        response.put("works", work.map(List::of).orElse(List.of()));

        return ApiResponse.ok(response);
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> getWorks(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "updatedAt") String sort,
            @RequestParam(defaultValue = "desc") String order) {

        Sort.Direction direction = order.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(direction, sort));

        Page<WorkResponse> works = workService.getWorks(userId, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("works", works.getContent());
        response.put("pagination", Map.of(
                "page", page,
                "limit", limit,
                "total", works.getTotalElements(),
                "totalPages", works.getTotalPages()));

        return ApiResponse.ok(response);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WorkResponse> createWork(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateWorkRequest request) {
        WorkResponse work = workService.createWork(userId, request);
        return ApiResponse.created(work);
    }

    @GetMapping("/{id}")
    public ApiResponse<WorkResponse> getWork(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        WorkResponse work = workService.getWork(userId, id);
        return ApiResponse.ok(work);
    }

    @PatchMapping("/{id}")
    public ApiResponse<WorkResponse> updateWork(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @RequestBody UpdateWorkRequest request) {
        WorkResponse work = workService.updateWork(userId, id, request);
        return ApiResponse.ok(work);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWork(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        workService.deleteWork(userId, id);
    }
}
