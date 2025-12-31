package com.stolink.backend.domain.discovery.service;

import com.stolink.backend.domain.chapter.entity.Chapter;
import com.stolink.backend.domain.chapter.repository.ChapterRepository;
import com.stolink.backend.domain.discovery.dto.*;
import com.stolink.backend.domain.work.entity.Genre;
import com.stolink.backend.domain.work.entity.Work;
import com.stolink.backend.domain.work.repository.WorkRepository;
import com.stolink.backend.global.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiscoveryService {

    private final WorkRepository workRepository;
    private final ChapterRepository chapterRepository;

    public Page<DiscoveryWorkResponse> getWorks(Pageable pageable) {
        return workRepository.findAll(pageable)
                .map(work -> {
                    int chapterCount = chapterRepository.countByWorkId(work.getId());
                    return DiscoveryWorkResponse.from(work, chapterCount);
                });
    }

    public Page<DiscoveryWorkResponse> searchWorks(String keyword, Pageable pageable) {
        return workRepository.searchByKeyword(keyword, pageable)
                .map(work -> {
                    int chapterCount = chapterRepository.countByWorkId(work.getId());
                    return DiscoveryWorkResponse.from(work, chapterCount);
                });
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

        // 좋아요 정보 (ChapterLike 기능 제거됨 - 추후 다시 구현 필요시 추가)
        long likeCount = 0L;
        boolean likedByMe = false;

        // 이전/다음 챕터
        UUID workId = chapter.getWork().getId();
        int currentNumber = chapter.getChapterNumber();
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

