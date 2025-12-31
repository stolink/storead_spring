package com.stolink.backend.domain.chapter.dto;

import jakarta.validation.constraints.NotBlank;
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
}
