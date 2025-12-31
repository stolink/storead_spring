package com.stolink.backend.domain.rating.repository;

import com.stolink.backend.domain.rating.entity.ChapterRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChapterRatingRepository extends JpaRepository<ChapterRating, UUID> {

    Optional<ChapterRating> findByChapterIdAndUserId(UUID chapterId, UUID userId);

    boolean existsByChapterIdAndUserId(UUID chapterId, UUID userId);

    void deleteByChapterId(UUID chapterId);
}
