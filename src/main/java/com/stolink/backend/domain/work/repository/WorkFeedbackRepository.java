package com.stolink.backend.domain.work.repository;

import com.stolink.backend.domain.work.entity.FeedbackType;
import com.stolink.backend.domain.work.entity.WorkFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface WorkFeedbackRepository extends JpaRepository<WorkFeedback, UUID> {
    
    @Query("SELECT f.type, COUNT(f) FROM WorkFeedback f WHERE f.work.id = :workId GROUP BY f.type")
    List<Object[]> countByWorkIdGroupByType(@Param("workId") UUID workId);

    List<WorkFeedback> findByWorkIdAndUserId(UUID workId, UUID userId);
    
    boolean existsByWorkIdAndUserIdAndType(UUID workId, UUID userId, FeedbackType type);
}
