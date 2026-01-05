package com.stolink.backend.domain.work.entity;

import com.stolink.backend.domain.user.entity.User;
import com.stolink.backend.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "works")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Work extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String synopsis;

    @Column(length = 512)
    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Genre genre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WorkStatus status = WorkStatus.ONGOING;

    @Column(name = "project_id", unique = true)
    private String projectId;

    @Column(nullable = false)
    @Builder.Default
    private Long ratingSum = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long ratingCount = 0L;

    /**
     * 평균 별점 (인덱스 정렬 최적화용)
     * - 별점 등록/수정/삭제 시 자동 계산
     */
    @Column(nullable = false)
    @Builder.Default
    private Double averageRating = 0.0;

    public void update(String title, String synopsis, String coverImageUrl, Genre genre, WorkStatus status) {
        if (title != null)
            this.title = title;
        if (synopsis != null)
            this.synopsis = synopsis;
        if (coverImageUrl != null)
            this.coverImageUrl = coverImageUrl;
        if (genre != null)
            this.genre = genre;
        if (status != null)
            this.status = status;
    }

    public void addRating(int score) {
        this.ratingSum += score;
        this.ratingCount++;
        updateAverageRating();
    }

    public void updateRating(int oldScore, int newScore) {
        this.ratingSum = this.ratingSum - oldScore + newScore;
        updateAverageRating();
    }

    public void removeRating(int score) {
        this.ratingSum -= score;
        if (this.ratingCount > 0) {
            this.ratingCount--;
        }
        updateAverageRating();
    }

    /**
     * 평균 별점 계산 및 업데이트
     */
    private void updateAverageRating() {
        if (this.ratingCount > 0) {
            this.averageRating = (double) this.ratingSum / this.ratingCount;
        } else {
            this.averageRating = 0.0;
        }
    }

    @Column(nullable = false)
    @Builder.Default
    private Long likeCount = 0L;

    public void addLike() {
        this.likeCount++;
    }

    public void removeLike() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void syncLikeCount(long count) {
        this.likeCount = count;
    }
}
