package com.stolink.backend.domain.community.service;

import com.stolink.backend.domain.chapter.entity.Chapter;
import com.stolink.backend.domain.chapter.repository.ChapterRepository;
import com.stolink.backend.domain.community.dto.CommunityPublishRequest;
import com.stolink.backend.domain.community.dto.CommunityPublishResponse;
import com.stolink.backend.domain.draft.entity.Draft;
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

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommunityService {

    private final DraftService draftService;
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

        // 3. 중복 게시 체크 (동일 Work 내 동일 documentId 존재 여부)
        if (chapterRepository.existsByWorkIdAndDocumentId(work.getId(), draft.getDocumentId())) {
            throw new com.stolink.backend.domain.community.exception.DuplicateChapterException("이미 게시된 챕터입니다.");
        }

        // 4. Chapter 생성
        Chapter chapter = createChapter(work, draft, request.getChapterNumber(), request.getTitle());

        // 5. Draft 삭제 (소유권 체크 포함)
        draftService.deleteById(request.getDraftId(), userId);

        log.info("Community publish completed: workId={}, chapterId={}", work.getId(), chapter.getId());

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

        Chapter chapter = Chapter.builder()
                .work(work)
                .title(chapterTitle)
                .content(draft.getContent())
                .chapterNumber(chapterNumber)
                .documentId(draft.getDocumentId())
                .graphSnapshot(draft.getGraphSnapshot())
                .build();

        log.info("Created new chapter: workId={}, chapterNumber={}, title={}", 
                work.getId(), chapterNumber, chapterTitle);
        return chapterRepository.save(chapter);
    }
}
