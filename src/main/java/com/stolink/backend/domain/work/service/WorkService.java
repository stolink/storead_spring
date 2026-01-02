package com.stolink.backend.domain.work.service;

import com.stolink.backend.domain.chapter.repository.ChapterRepository;
import com.stolink.backend.domain.user.entity.User;
import com.stolink.backend.domain.user.repository.UserRepository;
import com.stolink.backend.domain.work.dto.CreateWorkRequest;
import com.stolink.backend.domain.work.dto.UpdateWorkRequest;
import com.stolink.backend.domain.work.dto.WorkResponse;
import com.stolink.backend.domain.work.entity.Work;
import com.stolink.backend.domain.work.entity.WorkStatus;
import com.stolink.backend.domain.work.repository.WorkRepository;
import com.stolink.backend.global.common.exception.AccessDeniedException;
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

        Work work = Work.builder()
                .author(author)
                .title(request.getTitle())
                .synopsis(request.getSynopsis())
                .coverImageUrl(request.getCoverImageUrl())
                .genre(request.getGenre())
                .status(request.getStatus() != null ? request.getStatus() : WorkStatus.ONGOING)
                .characterGraphData(request.getCharacterGraphData())
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
                request.getCharacterGraphData()
        );

        int chapterCount = chapterRepository.countByWorkId(workId);
        return WorkResponse.from(work, chapterCount);
    }

    @Transactional
    public void deleteWork(UUID userId, UUID workId) {
        Work work = workRepository.findByIdAndAuthorId(workId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("작품을 찾을 수 없습니다: " + workId));

        workRepository.delete(work);
    }
}
