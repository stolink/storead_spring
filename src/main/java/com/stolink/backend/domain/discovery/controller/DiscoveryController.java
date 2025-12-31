package com.stolink.backend.domain.discovery.controller;

import com.stolink.backend.domain.discovery.dto.DiscoveryChapterDetailResponse;
import com.stolink.backend.domain.discovery.dto.DiscoveryWorkDetailResponse;
import com.stolink.backend.domain.discovery.dto.DiscoveryWorkResponse;
import com.stolink.backend.domain.discovery.service.DiscoveryService;
import com.stolink.backend.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/discovery")
@RequiredArgsConstructor
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    @GetMapping
    public ApiResponse<Map<String, Object>> getWorks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String order) {

        Sort.Direction direction = order.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(direction, sort));

        Page<DiscoveryWorkResponse> works = discoveryService.getWorks(pageable);

        return ApiResponse.ok(Map.of(
                "works", works.getContent(),
                "pagination", Map.of(
                        "page", page,
                        "size", size,
                        "total", works.getTotalElements(),
                        "totalPages", works.getTotalPages(),
                        "hasNext", works.hasNext()
                )
        ));
    }

    @GetMapping("/search")
    public ApiResponse<Map<String, Object>> searchWorks(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<DiscoveryWorkResponse> works = discoveryService.searchWorks(keyword, pageable);

        return ApiResponse.ok(Map.of(
                "works", works.getContent(),
                "keyword", keyword,
                "pagination", Map.of(
                        "page", page,
                        "size", size,
                        "total", works.getTotalElements(),
                        "totalPages", works.getTotalPages(),
                        "hasNext", works.hasNext()
                )
        ));
    }

    @GetMapping("/works/{id}")
    public ApiResponse<DiscoveryWorkDetailResponse> getWorkDetail(@PathVariable UUID id) {
        DiscoveryWorkDetailResponse work = discoveryService.getWorkDetail(id);
        return ApiResponse.ok(work);
    }

    @GetMapping("/chapters/{id}")
    public ApiResponse<DiscoveryChapterDetailResponse> getChapterDetail(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        DiscoveryChapterDetailResponse chapter = discoveryService.getChapterDetail(id, userId);
        return ApiResponse.ok(chapter);
    }
}
