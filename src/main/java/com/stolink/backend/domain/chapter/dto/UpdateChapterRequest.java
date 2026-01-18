package com.stolink.backend.domain.chapter.dto;

import com.stolink.backend.domain.chapter.entity.ChapterAccessType;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateChapterRequest {
    private String title;
    private String content;

    // 유료/무료 관련 필드 (선택적 업데이트)
    private Boolean isFree;

    @Min(value = 0, message = "가격은 0 이상이어야 합니다")
    private Integer price;

    private ChapterAccessType accessType;
}
