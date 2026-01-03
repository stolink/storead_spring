package com.stolink.backend.domain.work.dto;

import com.stolink.backend.domain.work.entity.Genre;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateWorkRequest {

    @NotBlank(message = "제목은 필수입니다")
    private String title;

    @NotBlank(message = "줄거리는 필수입니다")
    private String synopsis;

    private String coverImageUrl;

    @NotNull(message = "장르는 필수입니다")
    private Genre genre;

    private com.stolink.backend.domain.work.entity.WorkStatus status;

    private java.util.Map<String, Object> characterGraphData;
}
