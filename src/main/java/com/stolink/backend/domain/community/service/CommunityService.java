package com.stolink.backend.domain.community.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stolink.backend.domain.chapter.entity.Chapter;
import com.stolink.backend.domain.chapter.repository.ChapterRepository;
import com.stolink.backend.domain.community.dto.CommunityPublishRequest;
import com.stolink.backend.domain.community.dto.CommunityPublishResponse;
import com.stolink.backend.domain.draft.entity.Draft;
import com.stolink.backend.domain.draft.exception.DraftExpiredException;
import com.stolink.backend.domain.draft.repository.DraftRepository;
import com.stolink.backend.domain.user.entity.User;
import com.stolink.backend.domain.user.repository.UserRepository;
import com.stolink.backend.domain.work.entity.Genre;
import com.stolink.backend.domain.work.entity.Work;
import com.stolink.backend.domain.work.repository.WorkRepository;
import com.stolink.backend.global.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommunityService {

    private final DraftRepository draftRepository;
    private final WorkRepository workRepository;
    private final ChapterRepository chapterRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Draft 기반으로 Work(없으면 생성) + Chapter 생성
     */
    public CommunityPublishResponse publish(CommunityPublishRequest request) {
        // 1. Draft 조회
        Draft draft = draftRepository.findById(request.getDraftId())
                .orElseThrow(() -> new ResourceNotFoundException("Draft", "id", request.getDraftId()));

        if (draft.isExpired()) {
            throw new DraftExpiredException(request.getDraftId());
        }

        // 2. Work 조회 또는 생성
        AtomicBoolean workCreated = new AtomicBoolean(false);
        Work work = workRepository.findByProjectId(draft.getProjectId())
                .orElseGet(() -> {
                    workCreated.set(true);
                    return createWork(draft);
                });

        // 3. 중복 게시 체크 (동일 Work 내 동일 documentId 존재 여부)
        if (chapterRepository.existsByWorkIdAndDocumentId(work.getId(), draft.getDocumentId())) {
            throw new com.stolink.backend.domain.community.exception.DuplicateChapterException("이미 게시된 챕터입니다.");
        }

        // 4. Chapter 생성
        Chapter chapter = createChapter(work, draft, request.getChapterNumber(), request.getTitle());

        // 5. Work graphSnapshot 업데이트 (있는 경우)
        if (draft.getGraphSnapshot() != null) {
            String graphJson = toJsonString(draft.getGraphSnapshot());
            work.update(null, null, null, null, null, graphJson);
        }

        // 5. Draft 삭제
        draftRepository.delete(draft);

        log.info("Community publish success: workId={}, chapterId={}, workCreated={}",
                work.getId(), chapter.getId(), workCreated.get());

        return CommunityPublishResponse.builder()
                .workId(work.getId())
                .chapterId(chapter.getId())
                .workCreated(workCreated.get())
                .build();
    }

    private Work createWork(Draft draft) {
        User author = userRepository.findById(draft.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", draft.getUserId()));

        Genre genre = parseGenre(draft.getWorkGenre());

        Work work = Work.builder()
                .author(author)
                .projectId(draft.getProjectId())
                .title(draft.getWorkTitle() != null ? draft.getWorkTitle() : draft.getTitle())
                .synopsis(draft.getWorkSynopsis() != null ? draft.getWorkSynopsis() : "")
                .genre(genre)
                .coverImageUrl(draft.getWorkCoverUrl())
                .build();

        log.info("Created new work: projectId={}, title={}", draft.getProjectId(), work.getTitle());
        return workRepository.save(work);
    }

    private Chapter createChapter(Work work, Draft draft, Integer chapterNumber, String overrideTitle) {
        if (chapterNumber == null) {
            chapterNumber = chapterRepository.countByWorkId(work.getId()) + 1;
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
                .build();

        log.info("Created new chapter: workId={}, chapterNumber={}, title={}", 
                work.getId(), chapterNumber, chapterTitle);
        return chapterRepository.save(chapter);
    }

    private Genre parseGenre(String genreStr) {
        if (genreStr == null || genreStr.isBlank()) {
            return Genre.OTHER;
        }
        try {
            return Genre.valueOf(genreStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown genre: {}, using OTHER", genreStr);
            return Genre.OTHER;
        }
    }

    private String toJsonString(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert map to JSON string", e);
            return null;
        }
    }
}
