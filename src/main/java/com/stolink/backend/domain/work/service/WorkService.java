package com.stolink.backend.domain.work.service;

import com.stolink.backend.domain.chapter.repository.ChapterRepository;
import com.stolink.backend.domain.user.entity.User;
import com.stolink.backend.domain.user.repository.UserRepository;
import com.stolink.backend.domain.work.dto.CreateWorkRequest;
import com.stolink.backend.domain.work.dto.UpdateWorkRequest;
import com.stolink.backend.domain.work.dto.WorkResponse;
import com.stolink.backend.domain.work.entity.Work;
import com.stolink.backend.domain.work.entity.WorkStatus; // Added import for WorkStatus
import com.stolink.backend.domain.work.repository.WorkRepository;
import com.stolink.backend.global.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkService {

    private final WorkRepository workRepository;
    private final UserRepository userRepository;
    private final ChapterRepository chapterRepository;

    public Page<WorkResponse> getWorks(UUID userId, Pageable pageable) {
        return workRepository.findByAuthorId(userId, pageable)
                .map(work -> {
                    int chapterCount = chapterRepository.countByWorkId(work.getId());
                    return WorkResponse.from(work, chapterCount);
                });
    }

    public WorkResponse getWork(UUID userId, UUID workId) {
        Work work = workRepository.findByIdAndAuthorId(workId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("작품을 찾을 수 없습니다: " + workId));
        int chapterCount = chapterRepository.countByWorkId(workId);
        return WorkResponse.from(work, chapterCount);
    }

    @Transactional
    public WorkResponse createWork(UUID userId, CreateWorkRequest request) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        // 유료/무료 설정 처리
        Boolean isFree = request.getIsFree();
        com.stolink.backend.domain.chapter.entity.ChapterAccessType accessType = request.getAccessType();

        if (accessType != null) {
            isFree = (accessType == com.stolink.backend.domain.chapter.entity.ChapterAccessType.FREE);
        } else if (isFree != null) {
            accessType = isFree ? com.stolink.backend.domain.chapter.entity.ChapterAccessType.FREE
                    : com.stolink.backend.domain.chapter.entity.ChapterAccessType.PAID;
        } else {
            isFree = true;
            accessType = com.stolink.backend.domain.chapter.entity.ChapterAccessType.FREE;
        }

        Work work = Work.builder()
                .author(author)
                .title(request.getTitle())
                .synopsis(request.getSynopsis())
                .coverImageUrl(request.getCoverImageUrl())
                .genre(request.getGenre())
                .status(request.getStatus() != null ? request.getStatus() : WorkStatus.ONGOING)
                .projectId(request.getProjectId())
                .isFree(isFree)
                .accessType(accessType)
                .build();

        Work saved = workRepository.save(work);
        return WorkResponse.from(saved);
    }

    @Transactional
    public WorkResponse updateWork(UUID userId, UUID workId, UpdateWorkRequest request) {
        Work work = workRepository.findByIdAndAuthorId(workId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("작품을 찾을 수 없습니다: " + workId));

        work.update(
                request.getTitle(),
                request.getSynopsis(),
                request.getCoverImageUrl(),
                request.getGenre(),
                request.getStatus(),
                request.getIsFree(),
                request.getAccessType());

        int chapterCount = chapterRepository.countByWorkId(workId);
        return WorkResponse.from(work, chapterCount);
    }

    @Transactional
    public void deleteWork(UUID userId, UUID workId) {
        Work work = workRepository.findByIdAndAuthorId(workId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("작품을 찾을 수 없습니다: " + workId));

        workRepository.delete(work);
    }

    /**
     * projectId로 작품 조회 (커뮤니티 배포용)
     */
    public java.util.Optional<WorkResponse> findByProjectId(String projectId) {
        return workRepository.findByProjectId(projectId)
                .map(work -> {
                    int chapterCount = chapterRepository.countByWorkId(work.getId());
                    return WorkResponse.from(work, chapterCount);
                });
    }
}
