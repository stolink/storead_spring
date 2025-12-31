package com.stolink.backend.domain.library.dto;

import com.stolink.backend.domain.library.entity.Library;
import com.stolink.backend.domain.work.entity.Genre;
import com.stolink.backend.domain.work.entity.WorkStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class LibraryResponse {

    private UUID id;
    private UUID workId;
    private String workTitle;
    private String workSynopsis;
    private String workCoverImageUrl;
    private Genre workGenre;
    private WorkStatus workStatus;
    private String authorNickname;
    private LocalDateTime addedAt;

    public static LibraryResponse from(Library library) {
        return LibraryResponse.builder()
                .id(library.getId())
                .workId(library.getWork().getId())
                .workTitle(library.getWork().getTitle())
                .workSynopsis(library.getWork().getSynopsis())
                .workCoverImageUrl(library.getWork().getCoverImageUrl())
                .workGenre(library.getWork().getGenre())
                .workStatus(library.getWork().getStatus())
                .authorNickname(library.getWork().getAuthor().getNickname())
                .addedAt(library.getCreatedAt())
                .build();
    }
}
