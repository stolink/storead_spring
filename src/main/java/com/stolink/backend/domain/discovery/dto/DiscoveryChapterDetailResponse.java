package com.stolink.backend.domain.discovery.dto;

import com.stolink.backend.domain.chapter.entity.Chapter;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class DiscoveryChapterDetailResponse {

    private UUID id;
    private UUID workId;
    private String workTitle;
    private String title;
    private String content;
    private Integer chapterNumber;
    private Long viewCount;
    private long likeCount;
    private boolean likedByMe;
    private UUID prevChapterId;
    private UUID nextChapterId;
    private java.util.Map<String, Object> graphSnapshot;
    private LocalDateTime createdAt;

    public static DiscoveryChapterDetailResponse from(
            Chapter chapter,
            long likeCount,
            boolean likedByMe,
            UUID prevChapterId,
            UUID nextChapterId) {
        return DiscoveryChapterDetailResponse.builder()
                .id(chapter.getId())
                .workId(chapter.getWork().getId())
                .workTitle(chapter.getWork().getTitle())
                .title(chapter.getTitle())
                .content(chapter.getContent())
                .chapterNumber(chapter.getChapterNumber())
                .viewCount(chapter.getViewCount())
                .likeCount(likeCount)
                .likedByMe(likedByMe)
                .prevChapterId(prevChapterId)
                .nextChapterId(nextChapterId)
                .graphSnapshot(chapter.getGraphSnapshot())
                .createdAt(chapter.getCreatedAt())
                .build();
    }
}
