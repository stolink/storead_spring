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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DiscoveryWorkResponse from(Work work, int chapterCount) {
        return DiscoveryWorkResponse.builder()
                .id(work.getId())
                .title(work.getTitle())
                .synopsis(work.getSynopsis())
                .coverImageUrl(work.getCoverImageUrl())
                .genre(work.getGenre())
                .status(work.getStatus())
                .authorNickname(work.getAuthor().getNickname())
                .chapterCount(chapterCount)
                .createdAt(work.getCreatedAt())
                .updatedAt(work.getUpdatedAt())
                .build();
    }
}
