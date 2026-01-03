package com.stolink.backend.domain.discovery.dto;

import com.stolink.backend.domain.chapter.entity.Chapter;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class DiscoveryChapterResponse {

    private UUID id;
    private String title;
    private Integer chapterNumber;
    private Long viewCount;
    private Long ratingSum;
    private Long ratingCount;
    private LocalDateTime createdAt;

    public static DiscoveryChapterResponse from(Chapter chapter) {
        return DiscoveryChapterResponse.builder()
                .id(chapter.getId())
                .title(chapter.getTitle())
                .chapterNumber(chapter.getChapterNumber())
                .viewCount(chapter.getViewCount())
                .ratingSum(java.util.Objects.requireNonNullElse(chapter.getRatingSum(), 0L))
                .ratingCount(java.util.Objects.requireNonNullElse(chapter.getRatingCount(), 0L))
                .createdAt(chapter.getCreatedAt())
                .build();
    }
}
