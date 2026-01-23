package com.stolink.backend.domain.chapter.service;

import com.stolink.backend.domain.chapter.dto.*;
import com.stolink.backend.domain.chapter.entity.Chapter;
import com.stolink.backend.domain.chapter.event.ChapterDeletedEvent;
import com.stolink.backend.domain.chapter.repository.ChapterRepository;
import com.stolink.backend.domain.work.entity.Work;
import com.stolink.backend.domain.work.repository.WorkRepository;
import com.stolink.backend.global.common.exception.AccessDeniedException;
import com.stolink.backend.global.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final WorkRepository workRepository;
    private final ApplicationEventPublisher eventPublisher;

    public List<ChapterResponse> getChapters(UUID userId, UUID workId) {
        // 권한 검증: 해당 사용자의 작품인지 확인
        workRepository.findByIdAndAuthorId(workId, userId)
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

        // 유료/무료 설정 처리
        Boolean isFree = request.getIsFree() != null ? request.getIsFree() : true;
        Integer price = request.getPrice() != null ? request.getPrice() : 0;
        com.stolink.backend.domain.chapter.entity.ChapterAccessType accessType = request.getAccessType() != null
                ? request.getAccessType()
                : com.stolink.backend.domain.chapter.entity.ChapterAccessType.FREE;

        // 유료 챕터의 경우 가격이 0보다 커야 함
        if (!isFree && price <= 0) {
            price = 10; // 기본 유료 가격: 10크레딧 (100원)
        }

        Chapter chapter = Chapter.builder()
                .work(work)
                .title(request.getTitle())
                .content(request.getContent())
                .chapterNumber(chapterNumber)
                .isFree(isFree)
                .price(price)
                .accessType(accessType)
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

        // 제목/내용 업데이트
        chapter.update(request.getTitle(), request.getContent());

        // 유료/무료 설정 업데이트
        if (request.getIsFree() != null || request.getPrice() != null || request.getAccessType() != null) {
            chapter.updatePricing(request.getIsFree(), request.getPrice(), request.getAccessType());
        }

        return ChapterDetailResponse.from(chapter);
    }

    @Transactional
    public void deleteChapter(UUID userId, UUID chapterId) {
        // N+1 문제 해결을 위해 Work, Author를 함께 조회
        Chapter chapter = chapterRepository.findByIdWithWorkAndAuthor(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("챕터를 찾을 수 없습니다: " + chapterId));

        if (!chapter.getWork().getAuthor().getId().equals(userId)) {
            throw new AccessDeniedException("해당 챕터에 접근 권한이 없습니다");
        }

        UUID workId = chapter.getWork().getId();
        int deletedChapterNumber = chapter.getChapterNumber();

        // 삭제 전 documentIds 추출 (Stolink 동기화용)
        List<String> documentIds = chapter.getAllDocumentIds();
        log.info("[ChapterService] Deleting chapter: id={}, documentIds={}", chapterId, documentIds);

        chapterRepository.delete(chapter);

        // Stolink Document 게시 상태 업데이트 (이벤트 발행 - 트랜잭션 커밋 후 리스너에서 처리)
        if (!documentIds.isEmpty()) {
            eventPublisher.publishEvent(new ChapterDeletedEvent(documentIds));
        }

        // 삭제된 챕터 이후의 챕터들 번호 -1
        List<Chapter> chaptersToShift = chapterRepository
                .findByWorkIdAndChapterNumberGreaterThan(workId, deletedChapterNumber);
        for (Chapter c : chaptersToShift) {
            c.updateChapterNumber(c.getChapterNumber() - 1);
        }
    }
}
