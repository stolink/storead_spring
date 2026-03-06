package com.stolink.backend.domain.work.dto;

import com.stolink.backend.domain.work.entity.Genre;
import com.stolink.backend.domain.work.entity.Work;
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
    private String projectId; // stolink 프로젝트 ID (에디터 연결용)
    private String title;
    private String synopsis;
    private String coverImageUrl;
    private Genre genre;
    private com.stolink.backend.domain.work.entity.WorkStatus status;
    private int chapterCount;
    private Long ratingSum;
    private Long ratingCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 유료/무료 관련 필드
    private Boolean isFree;
    private com.stolink.backend.domain.chapter.entity.ChapterAccessType accessType;

    public static WorkResponse from(Work work) {
        return WorkResponse.builder()
                .id(work.getId())
                .authorId(work.getAuthor().getId())
                .authorNickname(work.getAuthor().getNickname())
                .projectId(work.getProjectId())
                .title(work.getTitle())
                .synopsis(work.getSynopsis())
                .coverImageUrl(work.getCoverImageUrl())
                .genre(work.getGenre())
                .status(work.getStatus())
                .chapterCount(0)
                .ratingSum(work.getRatingSum())
                .ratingCount(work.getRatingCount())
                .createdAt(work.getCreatedAt())
                .updatedAt(work.getUpdatedAt())
                .isFree(work.getIsFree())
                .accessType(work.getAccessType())
                .build();
    }

    public static WorkResponse from(Work work, int chapterCount) {
        return WorkResponse.builder()
                .id(work.getId())
                .authorId(work.getAuthor().getId())
                .authorNickname(work.getAuthor().getNickname())
                .projectId(work.getProjectId())
                .title(work.getTitle())
                .synopsis(work.getSynopsis())
                .coverImageUrl(work.getCoverImageUrl())
                .genre(work.getGenre())
                .status(work.getStatus())
                .chapterCount(chapterCount)
                .ratingSum(work.getRatingSum())
                .ratingCount(work.getRatingCount())
                .createdAt(work.getCreatedAt())
                .updatedAt(work.getUpdatedAt())
                .isFree(work.getIsFree())
                .accessType(work.getAccessType())
                .build();
    }
}
