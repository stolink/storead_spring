package com.stolink.backend.domain.library.service;

import com.stolink.backend.domain.library.dto.LibraryResponse;
import com.stolink.backend.domain.library.entity.Library;
import com.stolink.backend.domain.library.repository.LibraryRepository;
import com.stolink.backend.domain.user.entity.User;
import com.stolink.backend.domain.user.repository.UserRepository;
import com.stolink.backend.domain.work.entity.Work;
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
public class LibraryService {

    private final LibraryRepository libraryRepository;
    private final UserRepository userRepository;
    private final WorkRepository workRepository;

    public Page<LibraryResponse> getLibrary(UUID userId, Pageable pageable) {
        return libraryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(LibraryResponse::from);
    }

    @Transactional
    public LibraryResponse addToLibrary(UUID userId, UUID workId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        Work work = workRepository.findById(workId)
                .orElseThrow(() -> new ResourceNotFoundException("작품을 찾을 수 없습니다: " + workId));

        // 이미 추가된 경우 기존 항목 반환
        if (libraryRepository.existsByUserIdAndWorkId(userId, workId)) {
            Library existing = libraryRepository.findByUserIdAndWorkId(userId, workId).get();
            return LibraryResponse.from(existing);
        }

        Library library = Library.builder()
                .user(user)
                .work(work)
                .build();

        Library saved = libraryRepository.save(library);
        return LibraryResponse.from(saved);
    }

    @Transactional
    public void removeFromLibrary(UUID userId, UUID workId) {
        if (!libraryRepository.existsByUserIdAndWorkId(userId, workId)) {
            throw new ResourceNotFoundException("서재에서 해당 작품을 찾을 수 없습니다");
        }
        libraryRepository.deleteByUserIdAndWorkId(userId, workId);
    }

    public boolean isInLibrary(UUID userId, UUID workId) {
        return libraryRepository.existsByUserIdAndWorkId(userId, workId);
    }
}
