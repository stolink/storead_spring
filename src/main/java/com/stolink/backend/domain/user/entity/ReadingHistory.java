package com.stolink.backend.domain.user.entity;

import com.stolink.backend.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 읽기 기록 엔티티
 * 
 * 개인화 추천 및 "읽던 작품" 기능을 위해 사용자의 독서 이력을 저장합니다.
 * - 작품별로 마지막으로 읽은 챕터와 진행률을 저장합니다.
 */
@Entity
@Table(name = "reading_histories", indexes = {
        @Index(name = "idx_reading_history_user", columnList = "user_id"),
        @Index(name = "idx_reading_history_user_work", columnList = "user_id, work_id", unique = true),
        @Index(name = "idx_reading_history_last_read", columnList = "last_read_at")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ReadingHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "work_id", nullable = false)
    private UUID workId;

    @Column(name = "last_chapter_id", nullable = false)
    private UUID lastChapterId;

    @Column(name = "last_chapter_number", nullable = false)
    private Integer lastChapterNumber;

    @Column(nullable = false)
    private Integer progress; // 0-100% (작품 전체 진행률 또는 챕터 내 스크롤?) -> 여기선 챕터 번호 기반 작품 전체 진행률로 가정

    @Column(name = "last_read_at", nullable = false)
    private LocalDateTime lastReadAt;

    /**
     * 읽기 기록 업데이트
     */
    public void updateProgress(UUID lastChapterId, Integer lastChapterNumber, Integer progress) {
        this.lastChapterId = lastChapterId;
        this.lastChapterNumber = lastChapterNumber;
        this.progress = progress;
        this.lastReadAt = LocalDateTime.now();
    }
}
