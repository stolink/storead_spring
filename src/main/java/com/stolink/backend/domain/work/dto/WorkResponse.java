package com.stolink.backend.domain.work.dto;

import com.stolink.backend.domain.work.entity.Genre;
import com.stolink.backend.domain.work.entity.Work;
import com.stolink.backend.domain.work.entity.WorkStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class WorkResponse {

    private UUID id;
    private UUID authorId;
    private String authorNickname;
    private String title;
    private String synopsis;
    private String coverImageUrl;
    private Genre genre;
    private WorkStatus status;
    private String characterGraphData;
    private int chapterCount;
    private Long ratingSum;
    private Long ratingCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WorkResponse from(Work work) {
        return WorkResponse.builder()
                .id(work.getId())
                .authorId(work.getAuthor().getId())
                .authorNickname(work.getAuthor().getNickname())
                .title(work.getTitle())
                .synopsis(work.getSynopsis())
                .coverImageUrl(work.getCoverImageUrl())
                .genre(work.getGenre())
                .status(work.getStatus())
                .characterGraphData(work.getCharacterGraphData())
                .chapterCount(0)
                .ratingSum(work.getRatingSum())
                .ratingCount(work.getRatingCount())
                .createdAt(work.getCreatedAt())
                .updatedAt(work.getUpdatedAt())
                .build();
    }

    public static WorkResponse from(Work work, int chapterCount) {
        return WorkResponse.builder()
                .id(work.getId())
                .authorId(work.getAuthor().getId())
                .authorNickname(work.getAuthor().getNickname())
                .title(work.getTitle())
                .synopsis(work.getSynopsis())
                .coverImageUrl(work.getCoverImageUrl())
                .genre(work.getGenre())
                .status(work.getStatus())
                .characterGraphData(work.getCharacterGraphData())
                .chapterCount(chapterCount)
                .ratingSum(work.getRatingSum())
                .ratingCount(work.getRatingCount())
                .createdAt(work.getCreatedAt())
                .updatedAt(work.getUpdatedAt())
                .build();
    }
}
