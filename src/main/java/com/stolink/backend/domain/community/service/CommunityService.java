package com.stolink.backend.domain.community.service;

import com.stolink.backend.domain.chapter.entity.Chapter;
import com.stolink.backend.domain.chapter.repository.ChapterRepository;
import com.stolink.backend.domain.chapter.service.ChapterDuplicationChecker;
import com.stolink.backend.domain.community.dto.CommunityPublishRequest;
import com.stolink.backend.domain.community.dto.CommunityPublishResponse;
import com.stolink.backend.domain.draft.entity.Draft;
import com.stolink.backend.domain.draft.service.DocumentPublishService;
import com.stolink.backend.domain.draft.service.DraftService;
import com.stolink.backend.domain.user.entity.User;
import com.stolink.backend.domain.user.repository.UserRepository;
import com.stolink.backend.domain.work.entity.Genre;
import com.stolink.backend.domain.work.entity.Work;
import com.stolink.backend.domain.work.repository.WorkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import jakarta.persistence.PersistenceException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommunityService {

    private final DraftService draftService;
    private final DocumentPublishService documentPublishService;
    private final WorkRepository workRepository;
    private final ChapterRepository chapterRepository;
    private final UserRepository userRepository;
    private final ChapterDuplicationChecker duplicationChecker;

    /**
     * Draft 기반으로 Work(없으면 생성) + Chapter 생성
     */
    public CommunityPublishResponse publish(CommunityPublishRequest request, UUID userId) {
        log.info("[DEBUG][STEP 0] Starting publish process: draftId={}, userId={}", request.getDraftId(), userId);

        // 1. Draft 조회 (만료 및 소유권 체크 포함)
        Draft draft = draftService.findEntityById(request.getDraftId(), userId);
        log.info("[DEBUG][STEP 1] Draft found: projectId={}", draft.getProjectId());

        // 2. Work 조회 또는 생성 (동시성 제어 포함)
        AtomicBoolean workCreated = new AtomicBoolean(false);
        Work work = getOrCreateWork(draft, userId, workCreated);
        log.info("[DEBUG][STEP 2] Work processed: workId={}, created={}", work.getId(), workCreated.get());

        // 3. 중복 게시 체크 (일괄 쿼리로 N+1 방지)
        List<String> allDocumentIds = draft.getAllDocumentIds();
        log.info("[DEBUG][STEP 3] Checking duplication for workId={}, documentIds={}", work.getId(), allDocumentIds);

        List<String> duplicates = duplicationChecker.findDuplicates(work.getId(), allDocumentIds);
        if (!duplicates.isEmpty()) {
            log.error("[ERROR] Duplicate chapter detected: ids={}", duplicates);
            throw new com.stolink.backend.domain.community.exception.DuplicateChapterException(
                    "이미 게시된 챕터가 있습니다: " + String.join(", ", duplicates));
        }

        // 4. Chapter 생성 및 JSONB 데이터 검증
        Chapter chapter = createChapter(work, draft, request.getChapterNumber(), request.getTitle());
        if (chapter == null) {
            throw new RuntimeException("Chapter creation failed: null object returned.");
        }
        validateChapterMetadata(chapter);

        // 5. Chapter 저장 (강제 플러시 및 예외 포착)
        UUID chapterId;
        try {
            log.info("[DEBUG][STEP 4] Attempting to save and flush Chapter entity...");
            chapterRepository.saveAndFlush(chapter);

            // 저장 직후 ID 존재 여부 재확인
            chapterId = chapter.getId();
            if (chapterId == null || !chapterRepository.existsById(chapterId)) {
                log.error("[CRITICAL] saveAndFlush called but entity ID is null or not found in DB! id={}", chapterId);
                throw new RuntimeException("Chapter preservation failed during flush.");
            }
            log.info("[DEBUG][STEP 5] Chapter saved and flushed successfully. chapterId={}", chapterId);
        } catch (DataIntegrityViolationException | JpaSystemException | PersistenceException e) {
            log.error("[CRITICAL DB ERROR] Failed to persist chapter to database. StackTrace follows:", e);
            throw e; // 호출자에게 전파하여 트랜잭션 롤백 유도
        } catch (Exception e) {
            log.error("[UNEXPECTED ERROR] An unexpected error occurred during chapter save:", e);
            throw e;
        }

        // 6. Document 게시 상태 업데이트 (Stolink DB)
        log.info("[DEBUG][STEP 6] Updating document publish status...");
        documentPublishService.markAsPublished(draft.getAllDocumentIds());

        // 7. Draft 삭제 (소유권 체크 포함)
        log.info("[DEBUG][STEP 7] Deleting source draft: draftId={}", request.getDraftId());
        draftService.deleteById(request.getDraftId(), userId);

        log.info("[CommunityService] Community publish completed successfully. workId={}, chapterId={}",
                work.getId(), chapterId);

        return CommunityPublishResponse.builder()
                .workId(work.getId())
                .chapterId(chapterId)
                .workCreated(workCreated.get())
                .build();
    }

    /**
     * JSONB 컬럼 데이터 검증 (null 여부 및 기본 구조 확인)
     */
    private void validateChapterMetadata(Chapter chapter) {
        log.info("[DEBUG] Validating Chapter JSONB metadata...");

        // 1. graph_snapshot 검증 (Optional)
        Map<String, Object> graph = chapter.getGraphSnapshot();
        if (graph == null) {
            log.info("[VALIDATION] graph_snapshot is null (Allowed).");
        } else {
            log.info("[VALIDATION] graph_snapshot size: {}", graph.size());
        }

        // 2. document_id / document_ids 검증 (둘 중 하나는 존재해야 함)
        String singleId = chapter.getDocumentId();
        List<String> multiIds = chapter.getDocumentIds();

        if (singleId == null && (multiIds == null || multiIds.isEmpty())) {
            log.error("[VALIDATION ERROR] Both documentId and documentIds are null/empty.");
            throw new IllegalArgumentException("Chapter must have either documentId or documentIds.");
        }

        log.info("[VALIDATION] document identity check passed: singleId={}, multiIdsCount={}",
                singleId, (multiIds != null ? multiIds.size() : 0));
    }

    /**
     * 작품 조회 또는 생성 (동시성 요청에 의한 중복 생성 방지)
     */
    private Work getOrCreateWork(Draft draft, UUID userId, AtomicBoolean workCreated) {
        return workRepository.findByProjectId(draft.getProjectId())
                .orElseGet(() -> {
                    try {
                        workCreated.set(true);
                        Work newWork = createWork(draft, userId);
                        return workRepository.save(newWork);
                    } catch (DataIntegrityViolationException e) {
                        // 동시에 여러 요청이 올 경우 Unique 제약 조건 위반 발생 가능 -> 재조회
                        log.info(
                                "[CommunityService] Race condition detected during Work creation. Re-fetching existing work.");
                        workCreated.set(false);
                        return workRepository.findByProjectId(draft.getProjectId())
                                .orElseThrow(() -> new RuntimeException(
                                        "Work creation failed due to race condition, but still not found.", e));
                    }
                });
    }

    private Work createWork(Draft draft, UUID userId) {
        User author = userRepository.getReferenceById(userId);
        String title = draft.getWorkTitle() != null ? draft.getWorkTitle() : draft.getTitle();
        String synopsis = draft.getWorkSynopsis() != null ? draft.getWorkSynopsis() : "";
        Genre genre = Genre.from(draft.getWorkGenre());
        String coverUrl = draft.getWorkCoverUrl();

        Work work = Work.builder()
                .author(author)
                .title(title)
                .synopsis(synopsis)
                .genre(genre)
                .coverImageUrl(coverUrl)
                .projectId(draft.getProjectId())
                .build();

        log.info("[CommunityService] Created new work object: projectId={}, title={}", draft.getProjectId(), title);
        return work;
    }

    private Chapter createChapter(Work work, Draft draft, Integer requestedChapterNumber, String overrideTitle) {
        int chapterNumber;
        if (requestedChapterNumber != null) {
            chapterNumber = requestedChapterNumber;
        } else {
            chapterNumber = chapterRepository.findMaxChapterNumberByWorkId(work.getId())
                    .orElse(0) + 1;
        }

        String chapterTitle = (overrideTitle != null && !overrideTitle.isBlank())
                ? overrideTitle
                : draft.getTitle();

        // 병합 배포 여부에 따라 저장 필드 분기
        Chapter.ChapterBuilder builder = Chapter.builder()
                .work(work)
                .title(chapterTitle)
                .content(draft.getContent())
                .chapterNumber(chapterNumber)
                .graphSnapshot(draft.getGraphSnapshot());

        if (Boolean.TRUE.equals(draft.getIsMerged())) {
            // 시나리오 C: 병합 배포 → documentIds 배열에 저장
            builder.documentIds(draft.getAllDocumentIds());
            log.info("[CommunityService] Preparing merged chapter: chapterNumber={}, title={}, documentIds={}",
                    chapterNumber, chapterTitle, draft.getAllDocumentIds());
        } else {
            // 시나리오 A, B: 단일/각각 배포 → documentId에 저장
            String docId = draft.getDocumentId();
            if (docId == null && !draft.getAllDocumentIds().isEmpty()) {
                docId = draft.getAllDocumentIds().get(0);
            }
            builder.documentId(docId);
            log.info("[CommunityService] Preparing single chapter: chapterNumber={}, title={}, documentId={}",
                    chapterNumber, chapterTitle, docId);
        }

        return builder.build();
    }
}
