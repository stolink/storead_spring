package com.stolink.backend.domain.library.repository;

import com.stolink.backend.domain.library.entity.Library;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LibraryRepository extends JpaRepository<Library, UUID> {

    Page<Library> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<Library> findByUserIdAndWorkId(UUID userId, UUID workId);

    boolean existsByUserIdAndWorkId(UUID userId, UUID workId);

    void deleteByUserIdAndWorkId(UUID userId, UUID workId);
}
