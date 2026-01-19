package com.stolink.backend.domain.stats.entity;

import com.stolink.backend.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * 작품 방문 로그 엔티티
 * - 유입 경로(source) 분석 및 UV 집계를 위해 사용
 */
@Entity
@Table(name = "visit_logs", indexes = {
        @Index(name = "idx_visit_log_work_date", columnList = "work_id, created_at"),
        @Index(name = "idx_visit_log_work_source", columnList = "work_id, source")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class VisitLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "work_id", nullable = false)
    private UUID workId;

    @Column(name = "user_id") // 비로그인 유저도 집계할 수 있으나, UV는 user_id나 session_id 필요. 여기선 user_id (Nullable)
    private UUID userId;

    @Column(name = "source")
    private String source; // e.g., "recommend", "search", "ranking"

}
