package com.stolink.backend.domain.chapter.entity;

import com.stolink.backend.domain.work.entity.Work;
import com.stolink.backend.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "chapters", uniqueConstraints = {
    @UniqueConstraint(name = "uk_chapter_work_document", columnNames = {"work_id", "document_id"}),
    @UniqueConstraint(name = "uk_chapter_work_number", columnNames = {"work_id", "chapter_number"})
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Chapter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_id", nullable = false)
    private Work work;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Integer chapterNumber;

    @Column(name = "document_id", length = 255)
    private String documentId;

    @Column(nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long ratingSum = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long ratingCount = 0L;

    public void update(String title, String content) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
    }

    public void updateChapterNumber(Integer chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public void incrementViewCount() {
        this.viewCount++;
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
