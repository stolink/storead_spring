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

    @Column(columnDefinition = "TEXT")
    private String characterGraphData;

    @Column(nullable = false)
    @Builder.Default
    private Long ratingSum = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long ratingCount = 0L;

    public void update(String title, String synopsis, String coverImageUrl, Genre genre, WorkStatus status, String characterGraphData) {
        if (title != null) this.title = title;
        if (synopsis != null) this.synopsis = synopsis;
        if (coverImageUrl != null) this.coverImageUrl = coverImageUrl;
        if (genre != null) this.genre = genre;
        if (status != null) this.status = status;
        if (characterGraphData != null) this.characterGraphData = characterGraphData;
    }

    public void addRating(int score) {
        this.ratingSum += score;
        this.ratingCount++;
    }

    public void updateRating(int oldScore, int newScore) {
        this.ratingSum = this.ratingSum - oldScore + newScore;
    }

    public void removeRating(int score) {
        this.ratingSum -= score;
        if (this.ratingCount > 0) {
            this.ratingCount--;
        }
    }
}
