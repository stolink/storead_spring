package com.stolink.backend.domain.chapter.dto;

import com.stolink.backend.domain.chapter.entity.Chapter;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class ChapterDetailResponse {

    private UUID id;
    private UUID workId;
    private String title;
    private String content;
    private Integer chapterNumber;
    private Long viewCount;
    private Map<String, Object> graphSnapshot;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 유료/무료 관련 필드 (DiscoveryChapterDetailResponse와 통일)
    private Boolean isFree;
    private Integer price;
    private com.stolink.backend.domain.chapter.entity.ChapterAccessType accessType;
    private Boolean isPurchased;
    private Boolean hasAccess;

    public static ChapterDetailResponse from(Chapter chapter) {
        return from(chapter, false, true); // 기본적으로 접근 가능하다고 가정 (작가용)
    }

    public static ChapterDetailResponse from(Chapter chapter, boolean isPurchased, boolean hasAccess) {
        return ChapterDetailResponse.builder()
                .id(chapter.getId())
                .workId(chapter.getWork().getId())
                .title(chapter.getTitle())
                .content(hasAccess ? chapter.getContent() : "")
                .chapterNumber(chapter.getChapterNumber())
                .viewCount(chapter.getViewCount())
                .graphSnapshot(hasAccess ? chapter.getGraphSnapshot() : null)
                .createdAt(chapter.getCreatedAt())
                .updatedAt(chapter.getUpdatedAt())
                .isFree(chapter.getIsFree())
                .price(chapter.getPrice())
                .accessType(chapter.getAccessType())
                .isPurchased(isPurchased)
                .hasAccess(hasAccess)
                .build();
    }
}
