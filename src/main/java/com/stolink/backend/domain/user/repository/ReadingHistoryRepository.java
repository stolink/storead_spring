package com.stolink.backend.domain.user.repository;

import com.stolink.backend.domain.user.entity.ReadingHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory, UUID> {

    // 사용자의 특정 작품 읽기 기록 조회
    Optional<ReadingHistory> findByUserIdAndWorkId(UUID userId, UUID workId);

    // 사용자의 최근 읽은 작품 목록 조회 (Pageable로 개수 제한)
    @Query("SELECT rh FROM ReadingHistory rh WHERE rh.userId = :userId ORDER BY rh.lastReadAt DESC")
    List<ReadingHistory> findRecentByUserId(@Param("userId") UUID userId, Pageable pageable);

    // 사용자가 읽은 작품 목록 전체 조회 (추천 시스템 분석용)
    List<ReadingHistory> findAllByUserId(UUID userId);
}
