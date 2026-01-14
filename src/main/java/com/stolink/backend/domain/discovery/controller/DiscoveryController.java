package com.stolink.backend.domain.discovery.controller;

import com.stolink.backend.domain.discovery.dto.DiscoveryChapterDetailResponse;
import com.stolink.backend.domain.discovery.dto.DiscoveryWorkDetailResponse;
import com.stolink.backend.domain.discovery.dto.DiscoveryWorkResponse;
import com.stolink.backend.domain.discovery.service.DiscoveryService;
import com.stolink.backend.domain.discovery.service.RecommendationService;
import com.stolink.backend.global.common.dto.ApiResponse;
import com.stolink.backend.global.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/discovery")
@RequiredArgsConstructor
public class DiscoveryController {

        private final DiscoveryService discoveryService;
        private final RecommendationService recommendationService;

        @GetMapping("/continue-reading")
        public ApiResponse<java.util.List<com.stolink.backend.domain.discovery.dto.ContinueReadingResponse>> getContinueReading(
                        @AuthenticationPrincipal Object principal) {

                UUID userId = SecurityUtils.extractUserId(principal);

                if (userId == null) {
                        return ApiResponse.ok(java.util.Collections.emptyList());
                }

                return ApiResponse.ok(recommendationService.getContinueReading(userId));
        }

        @GetMapping("/recommendations")
        public ApiResponse<java.util.List<DiscoveryWorkResponse>> getRecommendations(
                        @AuthenticationPrincipal Object principal) {

                UUID userId = SecurityUtils.extractUserId(principal);

                if (userId == null) {
                        return ApiResponse.ok(java.util.Collections.emptyList());
                }

                return ApiResponse.ok(recommendationService.getTagBasedRecommendations(userId));
        }

        @GetMapping({ "", "/works" })
        public ApiResponse<Map<String, Object>> getWorks(
                        @RequestParam(required = false) java.util.List<String> genres,
                        @RequestParam(required = false) String status,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(defaultValue = "createdAt") String sort,
                        @RequestParam(defaultValue = "desc") String order) {

                log.info("Discovery API: getWorks requested. genres={}, status={}, page={}, size={}, sort={}, order={}",
                                genres, status, page, size, sort, order);

                Sort.Direction direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
                String sortProperty;

                // 정렬 필드 매핑
                if (sort == null) {
                    sortProperty = "createdAt";
                } else {
                    String cleanSort = sort.trim().toLowerCase();
                    if ("latest".equals(cleanSort) || "createdat".equals(cleanSort)) {
                        sortProperty = "createdAt";
                    } else if ("popular".equals(cleanSort)) {
                        sortProperty = "likeCount";
                    } else if ("rating".equals(cleanSort)) {
                        sortProperty = "averageRating";
                    } else {
                        // 허용되지 않은 정렬 값은 기본값(createdAt)으로 처리하여 예외 방지
                        log.warn("Invalid sort property received: {}. Defaulting to createdAt.", sort);
                        sortProperty = "createdAt";
                    }
                }

                log.info("Mapped sort property: '{}' -> '{}'", sort, sortProperty);

                Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(direction, sortProperty));

                Page<DiscoveryWorkResponse> works = discoveryService.getWorks(genres, status, pageable);

                log.info("Discovery API: Found {} works. Sending response.", works.getTotalElements());

                return ApiResponse.ok(Map.of(
                                "works", works.getContent(),
                                "pagination", Map.of(
                                                "page", page,
                                                "size", size,
                                                "total", works.getTotalElements(),
                                                "totalPages", works.getTotalPages(),
                                                "hasNext", works.hasNext())));
        }

        @GetMapping("/rankings")
        public ApiResponse<Map<String, Object>> getRankings(
                        @RequestParam(defaultValue = "REALTIME") String period,
                        @RequestParam(required = false) String genre,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {

                log.info("Discovery API: getRankings requested. period={}, genre={}", period, genre);

                // 랭킹은 기본적으로 순서가 정해져 있으므로 Sort 파라미터 불필요 (Service에서 처리)
                Pageable pageable = PageRequest.of(page, Math.min(size, 100));

                Page<DiscoveryWorkResponse> works = discoveryService.getRankings(period, genre, pageable);

                return ApiResponse.ok(Map.of(
                                "works", works.getContent(),
                                "pagination", Map.of(
                                                "page", page,
                                                "size", size,
                                                "total", works.getTotalElements(),
                                                "totalPages", works.getTotalPages(),
                                                "hasNext", works.hasNext())));
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
                                                "hasNext", works.hasNext())));
        }

        /**
         * 작품 상세 조회
         * - @AuthenticationPrincipal: permitAll 엔드포인트에서도 동작
         * - 토큰이 없으면 userId = null
         */
        @GetMapping("/works/{id}")
        public ApiResponse<DiscoveryWorkDetailResponse> getWorkDetail(
                        @PathVariable UUID id,
                        @AuthenticationPrincipal Object principal) {
                UUID userId = SecurityUtils.extractUserId(principal);
                DiscoveryWorkDetailResponse work = discoveryService.getWorkDetail(id, userId);
                return ApiResponse.ok(work);
        }

        @GetMapping("/chapters/{id}")
        public ApiResponse<DiscoveryChapterDetailResponse> getChapterDetail(
                        @PathVariable UUID id,
                        @AuthenticationPrincipal Object principal) {
                UUID userId = SecurityUtils.extractUserId(principal);
                DiscoveryChapterDetailResponse chapter = discoveryService.getChapterDetail(id, userId);
                return ApiResponse.ok(chapter);
        }
}
