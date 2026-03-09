package com.stolink.backend.domain.chapter.dto;

import com.stolink.backend.domain.chapter.entity.ChapterAccessType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateChapterRequest {

    @NotBlank(message = "챕터 제목은 필수입니다")
    private String title;

    @NotBlank(message = "챕터 내용은 필수입니다")
    private String content;

    private Integer chapterNumber; // null이면 마지막에 추가

    // 유료/무료 관련 필드
    private Boolean isFree;

    @Min(value = 0, message = "가격은 0 이상이어야 합니다")
    private Integer price;

    private ChapterAccessType accessType;
}
