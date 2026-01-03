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
}
