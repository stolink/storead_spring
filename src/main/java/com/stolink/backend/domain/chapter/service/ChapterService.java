package com.stolink.backend.domain.chapter.service;

import com.stolink.backend.domain.chapter.dto.*;
import com.stolink.backend.domain.chapter.entity.Chapter;
import com.stolink.backend.domain.chapter.repository.ChapterRepository;
import com.stolink.backend.domain.work.entity.Work;
import com.stolink.backend.domain.work.repository.WorkRepository;
import com.stolink.backend.global.common.exception.AccessDeniedException;
import com.stolink.backend.global.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final WorkRepository workRepository;

    public List<ChapterResponse> getChapters(UUID userId, UUID workId) {
        Work work = workRepository.findByIdAndAuthorId(workId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("작품을 찾을 수 없습니다: " + workId));

        return chapterRepository.findByWorkIdOrderByChapterNumberAsc(workId)
                .stream()
                .map(ChapterResponse::from)
                .collect(Collectors.toList());
    }

    public ChapterDetailResponse getChapter(UUID userId, UUID chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("챕터를 찾을 수 없습니다: " + chapterId));

        if (!chapter.getWork().getAuthor().getId().equals(userId)) {
            throw new AccessDeniedException("해당 챕터에 접근 권한이 없습니다");
        }

        return ChapterDetailResponse.from(chapter);
    }

    @Transactional
    public ChapterResponse createChapter(UUID userId, UUID workId, CreateChapterRequest request) {
        Work work = workRepository.findByIdAndAuthorId(workId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("작품을 찾을 수 없습니다: " + workId));

        int chapterNumber;
        if (request.getChapterNumber() != null) {
            // 중간 삽입: 해당 위치 이후 챕터들 번호 +1
            chapterNumber = request.getChapterNumber();
            List<Chapter> chaptersToShift = chapterRepository
                    .findByWorkIdAndChapterNumberGreaterThanEqual(workId, chapterNumber);
            for (Chapter c : chaptersToShift) {
                c.updateChapterNumber(c.getChapterNumber() + 1);
            }
        } else {
            // 마지막에 추가
            chapterNumber = chapterRepository.findMaxChapterNumberByWorkId(workId)
                    .orElse(0) + 1;
        }

        Chapter chapter = Chapter.builder()
                .work(work)
                .title(request.getTitle())
                .content(request.getContent())
                .chapterNumber(chapterNumber)
                .build();

        Chapter saved = chapterRepository.save(chapter);
        return ChapterResponse.from(saved);
    }

    @Transactional
    public ChapterDetailResponse updateChapter(UUID userId, UUID chapterId, UpdateChapterRequest request) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("챕터를 찾을 수 없습니다: " + chapterId));

        if (!chapter.getWork().getAuthor().getId().equals(userId)) {
            throw new AccessDeniedException("해당 챕터에 접근 권한이 없습니다");
        }

        chapter.update(request.getTitle(), request.getContent());
        return ChapterDetailResponse.from(chapter);
    }

    @Transactional
    public void deleteChapter(UUID userId, UUID chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("챕터를 찾을 수 없습니다: " + chapterId));

        if (!chapter.getWork().getAuthor().getId().equals(userId)) {
            throw new AccessDeniedException("해당 챕터에 접근 권한이 없습니다");
        }

        UUID workId = chapter.getWork().getId();
        int deletedChapterNumber = chapter.getChapterNumber();

        chapterRepository.delete(chapter);

        // 삭제된 챕터 이후의 챕터들 번호 -1
        List<Chapter> chaptersToShift = chapterRepository
                .findByWorkIdAndChapterNumberGreaterThan(workId, deletedChapterNumber);
        for (Chapter c : chaptersToShift) {
            c.updateChapterNumber(c.getChapterNumber() - 1);
        }
    }
}
