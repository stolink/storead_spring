package com.stolink.backend.domain.chapter.entity;

import com.stolink.backend.domain.work.entity.Work;
import com.stolink.backend.global.common.entity.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "chapters", uniqueConstraints = {
        // uk_chapter_work_document 제거: documentIds JSONB 배열 지원을 위해 앱 레벨에서 중복 체크
        @UniqueConstraint(name = "uk_chapter_work_number", columnNames = { "work_id", "chapter_number" })
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

    // 시나리오 A, B용: 단일/각각 배포 시 사용
    @Column(name = "document_id", length = 255)
    private String documentId;

    // 시나리오 C용: 병합 배포 시 사용 (다중 문서 ID 배열)
    @Type(JsonType.class)
    @Column(name = "document_ids", columnDefinition = "jsonb")
    private List<String> documentIds;

    @Type(JsonType.class)
    @Column(name = "graph_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> graphSnapshot;

    @Column(nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long ratingSum = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long ratingCount = 0L;

    // === 유료/무료 관련 필드 ===
    @Column(nullable = false)
    @Builder.Default
    private Boolean isFree = true; // 무료 여부

    @Column(nullable = false)
    @Builder.Default
    private Integer price = 0; // 크레딧 가격

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ChapterAccessType accessType = ChapterAccessType.FREE; // 접근 유형

    public void update(String title, String content) {
        if (title != null)
            this.title = title;
        if (content != null)
            this.content = content;
    }

    public void updatePricing(Boolean isFree, Integer price, ChapterAccessType accessType) {
        if (isFree != null) {
            this.isFree = isFree;
        }
        if (price != null) {
            this.price = price;
        }
        if (accessType != null) {
            this.accessType = accessType;
        }
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

    /**
     * 호환성 레이어: 모든 documentId를 리스트로 반환
     * - 병합 배포(시나리오 C): documentIds 반환
     * - 단일/각각 배포(시나리오 A, B): documentId를 리스트로 감싸서 반환
     */
    public List<String> getAllDocumentIds() {
        if (documentIds != null && !documentIds.isEmpty()) {
            return documentIds;
        }
        return documentId != null ? List.of(documentId) : List.of();
    }
}
