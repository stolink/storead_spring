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
    private LocalDateTime createdAt;

    public static DiscoveryChapterResponse from(Chapter chapter) {
        return DiscoveryChapterResponse.builder()
                .id(chapter.getId())
                .title(chapter.getTitle())
                .chapterNumber(chapter.getChapterNumber())
                .viewCount(chapter.getViewCount())
                .createdAt(chapter.getCreatedAt())
                .build();
    }
}
