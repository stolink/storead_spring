package com.stolink.backend.domain.bookmark.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ReadingProgressResponse {

    private UUID workId;
    private UUID lastReadChapterId;
    private String lastReadChapterTitle;
    private Integer lastReadChapterNumber;
    private Integer scrollPosition;
    private int totalChapters;
    private int readChapters;

    public static ReadingProgressResponse of(
            UUID workId,
            UUID lastReadChapterId,
            String lastReadChapterTitle,
            Integer lastReadChapterNumber,
            Integer scrollPosition,
            int totalChapters,
            int readChapters) {
        return ReadingProgressResponse.builder()
                .workId(workId)
                .lastReadChapterId(lastReadChapterId)
                .lastReadChapterTitle(lastReadChapterTitle)
                .lastReadChapterNumber(lastReadChapterNumber)
                .scrollPosition(scrollPosition)
                .totalChapters(totalChapters)
                .readChapters(readChapters)
                .build();
    }
}
