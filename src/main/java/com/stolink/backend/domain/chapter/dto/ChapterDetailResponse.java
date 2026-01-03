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

    public static ChapterDetailResponse from(Chapter chapter) {
        return ChapterDetailResponse.builder()
                .id(chapter.getId())
                .workId(chapter.getWork().getId())
                .title(chapter.getTitle())
                .content(chapter.getContent())
                .chapterNumber(chapter.getChapterNumber())
                .viewCount(chapter.getViewCount())
                .graphSnapshot(chapter.getGraphSnapshot())
                .createdAt(chapter.getCreatedAt())
                .updatedAt(chapter.getUpdatedAt())
                .build();
    }
}
