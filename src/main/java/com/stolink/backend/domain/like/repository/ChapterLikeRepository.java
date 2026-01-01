package com.stolink.backend.domain.like.repository;

import com.stolink.backend.domain.like.entity.ChapterLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * 챕터 좋아요 Repository
 * 챕터별 좋아요 정보 조회 및 관리를 담당합니다.
 */
public interface ChapterLikeRepository extends JpaRepository<ChapterLike, UUID> {

    /**
     * 특정 챕터와 사용자로 좋아요 조회
     */
    Optional<ChapterLike> findByChapterIdAndUserId(UUID chapterId, UUID userId);

    /**
     * 특정 챕터에 대한 사용자의 좋아요 존재 여부 확인
     */
    boolean existsByChapterIdAndUserId(UUID chapterId, UUID userId);

    /**
     * 특정 챕터의 좋아요 수 조회
     */
    long countByChapterId(UUID chapterId);

    /**
     * 특정 챕터의 모든 좋아요 삭제
     */
    void deleteByChapterId(UUID chapterId);
}
