package com.stolink.backend.domain.like.repository;

import com.stolink.backend.domain.like.entity.WorkLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * 작품 좋아요 Repository
 */
public interface WorkLikeRepository extends JpaRepository<WorkLike, UUID> {

    /**
     * 특정 유저가 특정 작품에 좋아요를 했는지 조회
     */
    Optional<WorkLike> findByUserIdAndWorkId(UUID userId, UUID workId);

    /**
     * 특정 유저가 특정 작품에 좋아요를 했는지 여부 확인
     */
    boolean existsByUserIdAndWorkId(UUID userId, UUID workId);

    /**
     * 특정 작품의 총 좋아요 수 조회
     */
    long countByWorkId(UUID workId);
}
