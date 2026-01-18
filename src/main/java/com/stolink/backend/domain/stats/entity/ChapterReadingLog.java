package com.stolink.backend.domain.stats.entity;

import com.stolink.backend.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * 챕터 열람 로그 엔티티
 * - 연독률(Retention) 분석을 위해 사용
 */
@Entity
@Table(name = "chapter_reading_logs", indexes = {
        @Index(name = "idx_chapter_log_work_chapter", columnList = "work_id, chapter_number"),
        @Index(name = "idx_chapter_log_created_at", columnList = "created_at")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ChapterReadingLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "work_id", nullable = false)
    private UUID workId;

    @Column(name = "chapter_id", nullable = false)
    private UUID chapterId;

    @Column(name = "chapter_number", nullable = false)
    private Integer chapterNumber;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

}
