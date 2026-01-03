package com.stolink.backend.domain.draft.dto;

import com.stolink.backend.domain.draft.entity.Draft;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class DraftResponse {

    private UUID id;
    private String documentId;
    private String projectId;
    private String title;
    private String content;
    private Map<String, Object> graphSnapshot;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    // Work 생성용 필드
    private String workTitle;
    private String workSynopsis;
    private String workGenre;
    private String workCoverUrl;

    public static DraftResponse from(Draft draft) {
        return DraftResponse.builder()
                .id(draft.getId())
                .documentId(draft.getDocumentId())
                .projectId(draft.getProjectId())
                .title(draft.getTitle())
                .content(draft.getContent())
                .graphSnapshot(draft.getGraphSnapshot())
                .createdAt(draft.getCreatedAt())
                .expiresAt(draft.getExpiresAt())
                .workTitle(draft.getWorkTitle())
                .workSynopsis(draft.getWorkSynopsis())
                .workGenre(draft.getWorkGenre())
                .workCoverUrl(draft.getWorkCoverUrl())
                .build();
    }
}
