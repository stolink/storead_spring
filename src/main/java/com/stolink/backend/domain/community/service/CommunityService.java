package com.stolink.backend.domain.community.service;

import com.stolink.backend.domain.chapter.entity.Chapter;
import com.stolink.backend.domain.chapter.repository.ChapterRepository;
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

import java.util.List;
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

    /**
     * Draft 기반으로 Work(없으면 생성) + Chapter 생성
     */
    public CommunityPublishResponse publish(CommunityPublishRequest request, UUID userId) {
        log.info("Starting community publish process: request={}, userId={}", request, userId);

        // 1. Draft 조회 (만료 및 소유권 체크 포함)
        Draft draft = draftService.findEntityById(request.getDraftId(), userId);

        // 2. Work 조회 또는 생성
        AtomicBoolean workCreated = new AtomicBoolean(false);
        Work work = workRepository.findByProjectId(draft.getProjectId())
                .orElseGet(() -> {
                    workCreated.set(true);
                    return createWork(draft, userId);
                });

        // 3. 중복 게시 체크 (일괄 쿼리로 N+1 방지)
        List<String> allDocumentIds = draft.getAllDocumentIds();
        String[] docIdsArray = allDocumentIds.toArray(new String[0]);
        log.info("Checking duplication for workId={}, documentIds={}", work.getId(), allDocumentIds);

        if (chapterRepository.existsByWorkIdAndAnyDocumentIds(work.getId(), docIdsArray)) {
            // 구체적인 중복 ID를 찾아서 에러 메시지에 포함
            List<String> duplicates = chapterRepository.findDuplicateDocumentIds(work.getId(), docIdsArray);
            log.warn("Duplicate chapters detected: workId={}, duplicateDocIds={}", work.getId(), duplicates);
            throw new com.stolink.backend.domain.community.exception.DuplicateChapterException(
                    "이미 게시된 챕터가 있습니다: " + String.join(", ", duplicates));
        }
        log.info("Duplication check passed for documentIds={}", allDocumentIds);

        // 4. Chapter 생성
        Chapter chapter = createChapter(work, draft, request.getChapterNumber(), request.getTitle());

        // 5. Document 게시 상태 업데이트 (Stolink DB)
        documentPublishService.markAsPublished(draft.getAllDocumentIds());

        // 6. Draft 삭제 (소유권 체크 포함)
        draftService.deleteById(request.getDraftId(), userId);

        log.info("Community publish completed: workId={}, chapterId={}, documentIds={}",
                work.getId(), chapter.getId(), draft.getAllDocumentIds());

        return CommunityPublishResponse.builder()
                .workId(work.getId())
                .chapterId(chapter.getId())
                .workCreated(workCreated.get())
                .build();
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

        log.info("Created new work: projectId={}, title={}, authorId={}", draft.getProjectId(), title, userId);
        return workRepository.save(work);
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
            log.info("Created merged chapter: workId={}, chapterNumber={}, title={}, documentIds={}",
                    work.getId(), chapterNumber, chapterTitle, draft.getAllDocumentIds());
        } else {
            // 시나리오 A, B: 단일/각각 배포 → documentId에 저장
            String docId = draft.getDocumentId();
            if (docId == null && !draft.getAllDocumentIds().isEmpty()) {
                docId = draft.getAllDocumentIds().get(0);
            }
            builder.documentId(docId);
            log.info("Created single chapter: workId={}, chapterNumber={}, title={}, documentId={}",
                    work.getId(), chapterNumber, chapterTitle, docId);
        }

        Chapter chapter = builder.build();
        return chapterRepository.save(chapter);
    }
}
