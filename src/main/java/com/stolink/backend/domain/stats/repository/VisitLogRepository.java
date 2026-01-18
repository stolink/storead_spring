package com.stolink.backend.domain.stats.repository;

import com.stolink.backend.domain.stats.entity.VisitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface VisitLogRepository extends JpaRepository<VisitLog, UUID> {

    @Query("SELECT v.source as source, COUNT(v) as count FROM VisitLog v WHERE v.workId = :workId GROUP BY v.source")
    List<SourceCount> findSourceCounts(@Param("workId") UUID workId);

    interface SourceCount {
        String getSource();

        Long getCount();
    }

    // 기간별 UV (필요시)
    @Query("SELECT COUNT(DISTINCT v.userId) FROM VisitLog v WHERE v.workId = :workId AND v.createdAt BETWEEN :start AND :end")
    Long countDistinctUsersByWorkIdAndDateRange(@Param("workId") UUID workId, @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
