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

    // stolink 프로젝트 연동용 (optional - storead 자체 생성 시 null 허용)
    private String projectId;

    // 유료/무료 관련 필드
    private Boolean isFree;
    private com.stolink.backend.domain.chapter.entity.ChapterAccessType accessType;
}
