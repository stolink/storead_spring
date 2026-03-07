package com.stolink.backend.domain.work.dto;

import com.stolink.backend.domain.work.entity.Genre;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateWorkRequest {
    private String title;
    private String synopsis;
    private String coverImageUrl;
    private Genre genre;
    private com.stolink.backend.domain.work.entity.WorkStatus status;
    private java.util.Map<String, Object> characterGraphData;

    // 유료/무료 관련 필드
    private Boolean isFree;
    private com.stolink.backend.domain.chapter.entity.ChapterAccessType accessType;
}
