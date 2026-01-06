package com.stolink.backend.domain.draft.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Draft 엔티티 - stolink에서 생성한 drafts 테이블과 매핑
 * storead에서는 조회/삭제 기능만 사용
 * 
 * @Immutable: 이 엔티티는 읽기/삭제 전용.
 *             Hibernate가 UPDATE 쿼리를 생성하지 않으며, DDL 변경 위험을 최소화.
 */
@Entity
@Table(name = "drafts")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Draft {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // 기존 단일 Document ID (하위 호환성 유지)
    @Column(name = "document_id")
    private String documentId;

    // 다중 Document ID 배열 (신규 Bulk 배포용)
    @Type(JsonType.class)
    @Column(name = "document_ids", columnDefinition = "jsonb")
    private List<String> documentIds;

    // 병합 배포 여부
    @Column(name = "is_merged")
    private Boolean isMerged;

    @Column(name = "project_id")
    private String projectId;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Type(JsonType.class)
    @Column(name = "graph_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> graphSnapshot;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // Work 생성용 필드 (Stolink에서 전달)
    @Column(name = "work_title")
    private String workTitle;

    @Column(name = "work_synopsis", columnDefinition = "TEXT")
    private String workSynopsis;

    @Column(name = "work_genre")
    private String workGenre;

    @Column(name = "work_cover_url")
    private String workCoverUrl;

    @Column(name = "publish_status")
    private String publishStatus;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 호환성 레이어: documentIds 조회 시 기존 documentId도 포함하여 반환
     */
    public List<String> getAllDocumentIds() {
        if (documentIds != null && !documentIds.isEmpty()) {
            return documentIds;
        }
        return documentId != null ? List.of(documentId) : List.of();
    }
}
