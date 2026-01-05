package com.stolink.backend.domain.chapter.dto;

import com.stolink.backend.domain.chapter.entity.Chapter;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ChapterResponse {

    private UUID id;
    private UUID workId;
    private String documentId; // stolink 문서 ID (에디터 연결용)
    private String title;
    private Integer chapterNumber;
    private Long viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ChapterResponse from(Chapter chapter) {
        return ChapterResponse.builder()
                .id(chapter.getId())
                .workId(chapter.getWork().getId())
                .documentId(chapter.getDocumentId())
                .title(chapter.getTitle())
                .chapterNumber(chapter.getChapterNumber())
                .viewCount(chapter.getViewCount())
                .createdAt(chapter.getCreatedAt())
                .updatedAt(chapter.getUpdatedAt())
                .build();
    }
}
