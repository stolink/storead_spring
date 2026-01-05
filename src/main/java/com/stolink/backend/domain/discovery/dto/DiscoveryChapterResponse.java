package com.stolink.backend.domain.discovery.dto;

import com.stolink.backend.domain.chapter.entity.Chapter;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class DiscoveryChapterResponse {

    private UUID id;
    private String title;
    private Integer chapterNumber;
    private Long viewCount;
    private Long ratingSum;
    private Long ratingCount;
    private LocalDateTime createdAt;

    // 유료/무료 관련 필드
    private Boolean isFree;
    private Integer price;
    private com.stolink.backend.domain.chapter.entity.ChapterAccessType accessType;
    @Builder.Default
    private Boolean isPurchased = false; // 기본값 false, 서비스에서 설정

    public static DiscoveryChapterResponse from(Chapter chapter) {
        return DiscoveryChapterResponse.builder()
                .id(chapter.getId())
                .title(chapter.getTitle())
                .chapterNumber(chapter.getChapterNumber())
                .viewCount(chapter.getViewCount())
                .ratingSum(java.util.Objects.requireNonNullElse(chapter.getRatingSum(), 0L))
                .ratingCount(java.util.Objects.requireNonNullElse(chapter.getRatingCount(), 0L))
                .createdAt(chapter.getCreatedAt())
                .isFree(chapter.getIsFree())
                .price(chapter.getPrice())
                .accessType(chapter.getAccessType())
                .build();
    }
}
