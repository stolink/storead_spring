package com.stolink.backend.domain.discovery.dto;

import com.stolink.backend.domain.chapter.entity.Chapter;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
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

    // 유료/무료 관련 필드
    private Boolean isFree;
    private Integer price;
    private com.stolink.backend.domain.chapter.entity.ChapterAccessType accessType;
    @Builder.Default
    private Boolean isPurchased = false;
    @Builder.Default
    private Boolean hasAccess = false; // 열람 권한 여부

    public static DiscoveryChapterDetailResponse from(
            Chapter chapter,
            long likeCount,
            boolean likedByMe,
            UUID prevChapterId,
            UUID nextChapterId,
            boolean hasAccess) {
        return DiscoveryChapterDetailResponse.builder()
                .id(chapter.getId())
                .workId(chapter.getWork().getId())
                .workTitle(chapter.getWork().getTitle())
                .title(chapter.getTitle())
                .content(hasAccess ? chapter.getContent() : "")
                .chapterNumber(chapter.getChapterNumber())
                .viewCount(chapter.getViewCount())
                .likeCount(likeCount)
                .likedByMe(likedByMe)
                .prevChapterId(prevChapterId)
                .nextChapterId(nextChapterId)
                .graphSnapshot(hasAccess ? chapter.getGraphSnapshot() : null)
                .createdAt(chapter.getCreatedAt())
                .isFree(chapter.getIsFree())
                .price(chapter.getPrice())
                .accessType(chapter.getAccessType())
                .hasAccess(hasAccess)
                .build();
    }
}
