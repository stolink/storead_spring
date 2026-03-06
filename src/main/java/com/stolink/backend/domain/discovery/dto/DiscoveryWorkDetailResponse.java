package com.stolink.backend.domain.discovery.dto;

import com.stolink.backend.domain.work.entity.Genre;
import com.stolink.backend.domain.work.entity.Work;
import com.stolink.backend.domain.work.entity.WorkStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class DiscoveryWorkDetailResponse {

    private UUID id;
    private String title;
    private String synopsis;
    private String coverImageUrl;
    private Genre genre;
    private WorkStatus status;
    private UUID authorId;
    private String authorNickname;
    private String authorAvatarUrl;
    private int chapterCount;
    private List<DiscoveryChapterResponse> chapters;
    private Long ratingSum;
    private Long ratingCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 유료/무료 관련 필드
    private Boolean isFree;
    private com.stolink.backend.domain.chapter.entity.ChapterAccessType accessType;

    // 좋아요 및 서재 관련 필드 (사용자별 상태)
    private Long likeCount; // 총 좋아요 수
    private Boolean isLiked; // 현재 사용자가 좋아요 했는지 (비로그인: null)
    private Boolean isInLibrary; // 현재 사용자가 서재에 추가했는지 (비로그인: null)

    public static DiscoveryWorkDetailResponse from(Work work, int chapterCount, List<DiscoveryChapterResponse> chapters,
            long likeCount, Boolean isLiked, Boolean isInLibrary) {
        return DiscoveryWorkDetailResponse.builder()
                .id(work.getId())
                .title(work.getTitle())
                .synopsis(work.getSynopsis())
                .coverImageUrl(work.getCoverImageUrl())
                .genre(work.getGenre())
                .status(work.getStatus())
                .authorId(work.getAuthor().getId())
                .authorNickname(work.getAuthor().getNickname())
                .authorAvatarUrl(work.getAuthor().getAvatarUrl())
                .chapterCount(chapterCount)
                .chapters(chapters)
                .ratingSum(work.getRatingSum())
                .ratingCount(work.getRatingCount())
                .createdAt(work.getCreatedAt())
                .updatedAt(work.getUpdatedAt())
                .isFree(work.getIsFree())
                .accessType(work.getAccessType())
                .likeCount(likeCount)
                .isLiked(isLiked)
                .isInLibrary(isInLibrary)
                .build();
    }
}
