package com.stolink.backend.domain.discovery.service;

import com.stolink.backend.domain.chapter.entity.Chapter;
import com.stolink.backend.domain.chapter.repository.ChapterRepository;
import com.stolink.backend.domain.discovery.dto.*;
import com.stolink.backend.domain.work.entity.Work;
import com.stolink.backend.domain.work.repository.WorkRepository;
import com.stolink.backend.global.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiscoveryService {

    private final WorkRepository workRepository;
    private final ChapterRepository chapterRepository;

    /**
     * 작품 목록 조회 (N+1 문제 해결 - 배치 조회)
     */
    public Page<DiscoveryWorkResponse> getWorks(Pageable pageable) {
        Page<Work> workPage = workRepository.findAll(pageable);
        return convertToResponse(workPage);
    }

    /**
     * 작품 검색 (N+1 문제 해결 - 배치 조회)
     */
    public Page<DiscoveryWorkResponse> searchWorks(String keyword, Pageable pageable) {
        Page<Work> workPage = workRepository.searchByKeyword(keyword, pageable);
        return convertToResponse(workPage);
    }

    /**
     * 작품 목록을 DiscoveryWorkResponse로 변환 (배치로 챕터 수 조회)
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
                        row -> (Long) row[1]
                ));

        List<DiscoveryWorkResponse> responses = works.stream()
                .map(work -> {
                    int chapterCount = chapterCountMap.getOrDefault(work.getId(), 0L).intValue();
                    return DiscoveryWorkResponse.from(work, chapterCount);
                })
                .collect(Collectors.toList());

        return new PageImpl<>(responses, workPage.getPageable(), workPage.getTotalElements());
    }

    public DiscoveryWorkDetailResponse getWorkDetail(UUID workId) {
        Work work = workRepository.findById(workId)
                .orElseThrow(() -> new ResourceNotFoundException("작품을 찾을 수 없습니다: " + workId));

        List<Chapter> chapters = chapterRepository.findByWorkIdOrderByChapterNumberAsc(workId);
        List<DiscoveryChapterResponse> chapterResponses = chapters.stream()
                .map(DiscoveryChapterResponse::from)
                .collect(Collectors.toList());

        return DiscoveryWorkDetailResponse.from(work, chapters.size(), chapterResponses);
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

        // 이전/다음 챕터
        UUID workId = chapter.getWork().getId();
        List<Chapter> allChapters = chapterRepository.findByWorkIdOrderByChapterNumberAsc(workId);

        UUID prevChapterId = null;
        UUID nextChapterId = null;
        for (int i = 0; i < allChapters.size(); i++) {
            if (allChapters.get(i).getId().equals(chapterId)) {
                if (i > 0) prevChapterId = allChapters.get(i - 1).getId();
                if (i < allChapters.size() - 1) nextChapterId = allChapters.get(i + 1).getId();
                break;
            }
        }

        return DiscoveryChapterDetailResponse.from(chapter, likeCount, likedByMe, prevChapterId, nextChapterId);
    }
}
