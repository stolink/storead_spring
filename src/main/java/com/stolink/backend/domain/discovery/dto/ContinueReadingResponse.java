package com.stolink.backend.domain.discovery.dto;

import com.stolink.backend.domain.user.entity.ReadingHistory;
import com.stolink.backend.domain.work.entity.Work;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ContinueReadingResponse {

    private UUID id; // history id
    private UUID workId;
    private UUID lastChapterId;
    private Integer lastChapterNumber;
    private Integer progress;
    private LocalDateTime lastReadAt;

    private DiscoveryWorkResponse work; // 작품 정보 포함

    public static ContinueReadingResponse of(ReadingHistory history, Work work) {
        return ContinueReadingResponse.builder()
                .id(history.getId())
                .workId(history.getWorkId())
                .lastChapterId(history.getLastChapterId())
                .lastChapterNumber(history.getLastChapterNumber())
                .progress(history.getProgress())
                .lastReadAt(history.getLastReadAt())
                .work(DiscoveryWorkResponse.from(work))
                .build();
    }
}
