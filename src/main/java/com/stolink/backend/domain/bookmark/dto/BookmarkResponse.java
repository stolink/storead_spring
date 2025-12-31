package com.stolink.backend.domain.bookmark.dto;

import com.stolink.backend.domain.bookmark.entity.Bookmark;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class BookmarkResponse {

    private UUID id;
    private UUID chapterId;
    private String chapterTitle;
    private Integer chapterNumber;
    private Integer scrollPosition;
    private LocalDateTime updatedAt;

    public static BookmarkResponse from(Bookmark bookmark) {
        return BookmarkResponse.builder()
                .id(bookmark.getId())
                .chapterId(bookmark.getChapter().getId())
                .chapterTitle(bookmark.getChapter().getTitle())
                .chapterNumber(bookmark.getChapter().getChapterNumber())
                .scrollPosition(bookmark.getScrollPosition())
                .updatedAt(bookmark.getUpdatedAt())
                .build();
    }
}
