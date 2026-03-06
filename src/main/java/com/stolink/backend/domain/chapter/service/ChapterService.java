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

        // 작가 또는 관리자만 접근 가능 (에디터/관리용)
        // 일반 독자는 DiscoveryService.getChapterDetail 사용
        if (!chapter.getWork().getAuthor().getId().equals(userId)) {
            throw new AccessDeniedException("해당 챕터에 접근 권한이 없습니다 (작가 전용)");
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

        // 유료/무료 설정 처리 (동기화 로직 포함)
        log.info("[ChapterService] createChapter request: isFree={}, price={}, accessType={}",
                request.getIsFree(), request.getPrice(), request.getAccessType());

        Integer price = request.getPrice() != null ? request.getPrice() : 0;
        com.stolink.backend.domain.chapter.entity.ChapterAccessType accessType = request.getAccessType();
        Boolean isFree = request.getIsFree();

        // accessType이 명시적으로 오면 우선순위
        if (accessType != null) {
            isFree = (accessType == com.stolink.backend.domain.chapter.entity.ChapterAccessType.FREE);
        } else if (isFree != null) {
            accessType = isFree ? com.stolink.backend.domain.chapter.entity.ChapterAccessType.FREE
                    : com.stolink.backend.domain.chapter.entity.ChapterAccessType.PAID;
        } else {
            // 둘 다 없으면 기본값
            isFree = (price <= 0);
            accessType = isFree ? com.stolink.backend.domain.chapter.entity.ChapterAccessType.FREE
                    : com.stolink.backend.domain.chapter.entity.ChapterAccessType.PAID;
        }

        // 유료 챕터의 경우 가격 보정
        if (!isFree && price <= 0) {
            price = 10; // 기본 유료 가격: 10크레딧
        }

        log.info("[ChapterService] Final pricing: isFree={}, price={}, accessType={}", isFree, price, accessType);

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
        log.info("[ChapterService] updateChapter request: id={}, isFree={}, price={}, accessType={}",
                chapterId, request.getIsFree(), request.getPrice(), request.getAccessType());

        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("챕터를 찾을 수 없습니다: " + chapterId));

        if (!chapter.getWork().getAuthor().getId().equals(userId)) {
            throw new AccessDeniedException("해당 챕터에 접근 권한이 없습니다");
        }

        // 제목/내용 업데이트
        chapter.update(request.getTitle(), request.getContent());

        // 유료/무료 설정 업데이트 (Entity 내부 동기화 로직 활용)
        if (request.getIsFree() != null || request.getPrice() != null || request.getAccessType() != null) {
            chapter.updatePricing(request.getIsFree(), request.getPrice(), request.getAccessType());
        }

        log.info("[ChapterService] Updated pricing in DB: isFree={}, price={}, accessType={}",
                chapter.getIsFree(), chapter.getPrice(), chapter.getAccessType());

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
