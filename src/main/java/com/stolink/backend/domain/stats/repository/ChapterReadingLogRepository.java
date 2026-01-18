package com.stolink.backend.domain.stats.repository;

import com.stolink.backend.domain.stats.entity.ChapterReadingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ChapterReadingLogRepository extends JpaRepository<ChapterReadingLog, UUID> {

    // 전체 누적 독자 수 (작품 기준)
    @Query("SELECT COUNT(DISTINCT c.userId) FROM ChapterReadingLog c WHERE c.workId = :workId")
    Long countDistinctUsersByWorkId(@Param("workId") UUID workId);

    // 기간별 독자 수 (성장률 계산용)
    @Query("SELECT COUNT(DISTINCT c.userId) FROM ChapterReadingLog c WHERE c.workId = :workId AND c.createdAt BETWEEN :start AND :end")
    Long countDistinctUsersByWorkIdAndDateRange(@Param("workId") UUID workId, @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // 챕터별 도달률 (Retention)
    @Query("SELECT c.chapterNumber as chapterNumber, COUNT(DISTINCT c.userId) as readerCount FROM ChapterReadingLog c WHERE c.workId = :workId GROUP BY c.chapterNumber ORDER BY c.chapterNumber ASC")
    List<ChapterReaderCount> findChapterReaderCounts(@Param("workId") UUID workId);

    interface ChapterReaderCount {
        Integer getChapterNumber();

        Long getReaderCount();
    }
}
