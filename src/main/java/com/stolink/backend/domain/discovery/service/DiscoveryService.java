package com.stolink.backend.domain.discovery.service;

import com.stolink.backend.domain.chapter.entity.Chapter;
import com.stolink.backend.domain.chapter.repository.ChapterRepository;
import com.stolink.backend.domain.discovery.dto.*;
import com.stolink.backend.domain.library.repository.LibraryRepository;
import com.stolink.backend.domain.like.repository.WorkLikeRepository;
import com.stolink.backend.domain.work.entity.Work;
import com.stolink.backend.domain.work.repository.WorkRepository;
import com.stolink.backend.global.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiscoveryService {

    private final WorkRepository workRepository;
    private final ChapterRepository chapterRepository;
    private final WorkLikeRepository workLikeRepository;
    private final LibraryRepository libraryRepository;
    private final com.stolink.backend.domain.chapter.repository.ChapterPurchaseRepository chapterPurchaseRepository; // 전체
                                                                                                                     // 경로
                                                                                                                     // 사용

    /**
     * 작품 목록 조회 (필터링 지원)
     */
    public Page<DiscoveryWorkResponse> getWorks(List<String> genres, String status, String accessType,
            Pageable pageable) {
        org.springframework.data.jpa.domain.Specification<Work> spec = (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            // 장르 필터
            if (genres != null && !genres.isEmpty()) {
                List<com.stolink.backend.domain.work.entity.Genre> genreEnums = genres.stream()
                        .map(com.stolink.backend.domain.work.entity.Genre::from)
                        .collect(Collectors.toList());
                predicates.add(root.get("genre").in(genreEnums));
            }

            // 상태 필터 (ONGOING, COMPLETED)
            if (status != null && !status.isEmpty()) {
                try {
                    com.stolink.backend.domain.work.entity.WorkStatus statusEnum = com.stolink.backend.domain.work.entity.WorkStatus
                            .valueOf(status.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("status"), statusEnum));
                } catch (IllegalArgumentException e) {
                    // Ignore invalid status
                }
            }

            // 접근 유형 필터 (FREE, PAID)
            if (accessType != null && !accessType.isEmpty()) {
                jakarta.persistence.criteria.Subquery<UUID> subquery = query.subquery(UUID.class);
                jakarta.persistence.criteria.Root<com.stolink.backend.domain.chapter.entity.Chapter> chapterRoot = subquery
                        .from(com.stolink.backend.domain.chapter.entity.Chapter.class);
                subquery.select(chapterRoot.get("work").get("id"));
                subquery.where(criteriaBuilder.equal(chapterRoot.get("isFree"), false));

                if ("FREE".equalsIgnoreCase(accessType)) {
                    // 유료 챕터가 하나도 없는 작품
                    predicates.add(criteriaBuilder.not(root.get("id").in(subquery)));
                } else if ("PAID".equalsIgnoreCase(accessType)) {
                    // 유료 챕터가 하나라도 있는 작품
                    predicates.add(root.get("id").in(subquery));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Work> workPage = workRepository.findAll(spec, pageable);
        return convertToResponse(workPage);
    }

    /**
     * 랭킹 조회
     */
    public Page<DiscoveryWorkResponse> getRankings(String period, String genre, String accessType, Pageable pageable) {
        Page<Work> workPage;

        // 장르 필터가 있으면 처리 (단, period 쿼리와 결합하기 복잡하므로 MVP에서는 ALLTIME 랭킹만 장르 지원하거나,
        // 단순하게 필터링 후 메모리 정렬은 비효율적.
        // 여기서는 "기간별 랭킹"은 전체 장르 대상으로 하고, "장르별 랭킹"은 ALLTIME(Sort)으로 처리하는 전략 사용 가능.
        // 하지만 요구사항은 "Ranking Page"에서 탭과 장르 필터가 있음.
        // findRankingByPeriod 쿼리에 genre 조건을 추가하는 것이 좋음. Repository 수정 필요할 수 있음.
        // 일단 기간 로직 구현.

        java.time.LocalDateTime startDate = null;
        if ("DAILY".equalsIgnoreCase(period)) {
            startDate = java.time.LocalDateTime.now().minusDays(1);
        } else if ("WEEKLY".equalsIgnoreCase(period)) {
            startDate = java.time.LocalDateTime.now().minusWeeks(1);
        } else if ("MONTHLY".equalsIgnoreCase(period)) {
            startDate = java.time.LocalDateTime.now().minusMonths(1);
        }

        if (startDate != null) {
            // 기간별 랭킹 (좋아요 급상승 등) - 필터 포함
            com.stolink.backend.domain.work.entity.Genre genreEnum = (genre != null && !genre.isEmpty()
                    && !"ALL".equalsIgnoreCase(genre))
                            ? com.stolink.backend.domain.work.entity.Genre.from(genre)
                            : null;

            workPage = workRepository.findRankingByPeriodWithFilter(startDate, genreEnum, accessType, pageable);
        } else {
            // 전체 기간 (= 실시간/누적 인기순) -> 좋아요 순 정렬 강제
            // DiscoveryController에서 Sort를 받아오더라도, 'RANKING' 로직에서는 likeCount DESC가 기본이어야 함.

            // 만약 period가 ALL-TIME이거나 null인데 랭킹 조회라면 likeCount 정렬을 강제하는 것이 안전.
            if ("ALL-TIME".equalsIgnoreCase(period) || "REALTIME".equalsIgnoreCase(period) || period == null) {
                // Pageable에서 Sort 재정의 필요
                pageable = org.springframework.data.domain.PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,
                                "likeCount"));
            }

            // 장르 및 AccessType 필터 적용
            org.springframework.data.jpa.domain.Specification<Work> spec = (root, query, criteriaBuilder) -> {
                List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

                // 장르 필터
                if (genre != null && !genre.isEmpty() && !"ALL".equalsIgnoreCase(genre)) {
                    com.stolink.backend.domain.work.entity.Genre genreEnum = com.stolink.backend.domain.work.entity.Genre
                            .from(genre);
                    predicates.add(criteriaBuilder.equal(root.get("genre"), genreEnum));
                }

                // AccessType 필터
                if (accessType != null && !accessType.isEmpty()) {
                    jakarta.persistence.criteria.Subquery<UUID> subquery = query.subquery(UUID.class);
                    jakarta.persistence.criteria.Root<com.stolink.backend.domain.chapter.entity.Chapter> chapterRoot = subquery
                            .from(com.stolink.backend.domain.chapter.entity.Chapter.class);
                    subquery.select(chapterRoot.get("work").get("id"));
                    subquery.where(criteriaBuilder.equal(chapterRoot.get("isFree"), false));

                    if ("FREE".equalsIgnoreCase(accessType)) {
                        predicates.add(criteriaBuilder.not(root.get("id").in(subquery)));
                    } else if ("PAID".equalsIgnoreCase(accessType)) {
                        predicates.add(root.get("id").in(subquery));
                    }
                }
                return criteriaBuilder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            };

            workPage = workRepository.findAll(spec, pageable);
        }

        return convertToResponse(workPage);
    }

    // 오버로딩 (기존 코드 호환용)
    public Page<DiscoveryWorkResponse> getWorks(Pageable pageable) {
        return getWorks(null, null, null, pageable);
    }

    /**
     * 작품 검색 (N+1 문제 해결 - 배치 조회)
     */
    public Page<DiscoveryWorkResponse> searchWorks(String keyword, Pageable pageable) {
        Page<Work> workPage = workRepository.searchByKeyword(keyword, pageable);
        return convertToResponse(workPage);
    }

    /**
     * 작품 목록을 DiscoveryWorkResponse로 변환 (배치로 챕터 수 조회, 좋아요 수는 Work 필드 사용)
     */
    private Page<DiscoveryWorkResponse> convertToResponse(Page<Work> workPage) {
        List<Work> works = workPage.getContent();

        if (works.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), workPage.getPageable(), workPage.getTotalElements());
        }

        // 배치로 모든 작품의 챕터 수를 한 번에 조회 (N+1 해결)
        List<UUID> workIds = works.stream()
                .map(Work::getId)
                .collect(Collectors.toList());

        Map<UUID, Long> chapterCountMap = workRepository.countChaptersByWorkIds(workIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]));

        List<DiscoveryWorkResponse> responses = works.stream()
                .map(work -> {
                    int chapterCount = chapterCountMap.getOrDefault(work.getId(), 0L).intValue();
                    // Work 엔티티의 관리되는 likeCount 사용
                    long likeCount = work.getLikeCount();
                    return DiscoveryWorkResponse.from(work, chapterCount, likeCount);
                })
                .collect(Collectors.toList());

        return new PageImpl<>(responses, workPage.getPageable(), workPage.getTotalElements());
    }

    /**
     * 작품 상세 조회
     * - userId가 null이면 비로그인 상태
     * - userId가 있으면 좋아요/서재 상태 조회
     */
    @Transactional(readOnly = true)
    public DiscoveryWorkDetailResponse getWorkDetail(UUID workId, UUID userId) {
        // [DEBUG] User ID 확인 로그
        log.info("[DiscoveryService] getWorkDetail called. workId: {}, userId: {}", workId, userId);

        Work work = workRepository.findById(workId)
                .orElseThrow(() -> new ResourceNotFoundException("작품을 찾을 수 없습니다: " + workId));

        List<Chapter> chapters = chapterRepository.findByWorkIdOrderByChapterNumberAsc(workId);
        List<DiscoveryChapterResponse> chapterResponses = chapters.stream()
                .map(DiscoveryChapterResponse::from)
                .collect(Collectors.toList());

        // 좋아요 수 조회
        long likeCount = workLikeRepository.countByWorkId(workId);

        // 사용자별 상태 조회 (로그인한 경우에만)
        Boolean isLiked = userId != null ? workLikeRepository.existsByUserIdAndWorkId(userId, workId) : null;
        Boolean isInLibrary = userId != null ? libraryRepository.existsByUserIdAndWorkId(userId, workId) : null;

        return DiscoveryWorkDetailResponse.from(work, chapters.size(), chapterResponses, likeCount, isLiked,
                isInLibrary);
    }

    @Transactional
    public DiscoveryChapterDetailResponse getChapterDetail(UUID chapterId, UUID userId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("챕터를 찾을 수 없습니다: " + chapterId));

        // 조회수 증가
        chapter.incrementViewCount();

        // TODO: 좋아요 기능 재구현 필요 - ChapterLike 엔티티 및 Repository 구현 후 활성화
        // ChapterLikeRepository 의존성 제거됨 - 추후 다시 구현 필요시 추가
        long likeCount = 0L;
        boolean likedByMe = false;

        // 이전/다음 챕터 (전체 목록 조회 대신 단건 조회 쿼리 사용으로 최적화)
        UUID workId = chapter.getWork().getId();
        int currentNumber = chapter.getChapterNumber();

        UUID prevChapterId = chapterRepository.findPrevChapterId(workId, currentNumber).orElse(null);
        UUID nextChapterId = chapterRepository.findNextChapterId(workId, currentNumber).orElse(null);

        DiscoveryChapterDetailResponse response = DiscoveryChapterDetailResponse.from(chapter, likeCount, likedByMe,
                prevChapterId, nextChapterId);

        // hasAccess 정합성 개선: 유료 챕터의 경우 실제 구매 여부 확인
        if (!Boolean.TRUE.equals(chapter.getIsFree()) && userId != null) {
            boolean isPurchased = chapterPurchaseRepository.existsByUserIdAndChapterId(userId, chapterId);

            // 유료이면서 구매했다면 열람 가능 (hasAccess = true)
            // 유료이면서 구매 안했으면 열람 불가 (hasAccess = false) - from 메서드에서 이미 false로 설정되어 있을
            // 것임(isFree가 false이므로)
            if (isPurchased) {
                response = response.toBuilder()
                        .isPurchased(true)
                        .hasAccess(true)
                        .build();
            }
        }

        return response;
    }
}
