package com.stolink.backend.domain.discovery.dto;

import com.stolink.backend.domain.work.entity.Genre;
import com.stolink.backend.domain.work.entity.Work;
import com.stolink.backend.domain.work.entity.WorkStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class DiscoveryWorkResponse {

    private UUID id;
    private String title;
    private String synopsis;
    private String coverImageUrl;
    private Genre genre;
    private WorkStatus status;
    private String authorNickname;
    private int chapterCount;
    private Long ratingSum;
    private Long ratingCount;
    private Long likeCount; // 좋아요 수 추가
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DiscoveryWorkResponse from(Work work, int chapterCount, long likeCount) {
        return DiscoveryWorkResponse.builder()
                .id(work.getId())
                .title(work.getTitle())
                .synopsis(work.getSynopsis())
                .coverImageUrl(work.getCoverImageUrl())
                .genre(work.getGenre())
                .status(work.getStatus())
                .authorNickname(work.getAuthor().getNickname())
                .chapterCount(chapterCount)
                .ratingSum(work.getRatingSum())
                .ratingCount(work.getRatingCount())
                .likeCount(likeCount)
                .createdAt(work.getCreatedAt())
                .updatedAt(work.getUpdatedAt())
                .build();
    }

    /**
     * 간단한 변환 (부가 정보 0으로 초기화)
     */
    public static DiscoveryWorkResponse from(Work work) {
        return from(work, 0, work.getLikeCount());
    }
}
