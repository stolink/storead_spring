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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DiscoveryWorkDetailResponse from(Work work, int chapterCount, List<DiscoveryChapterResponse> chapters) {
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
                .createdAt(work.getCreatedAt())
                .updatedAt(work.getUpdatedAt())
                .build();
    }
}
