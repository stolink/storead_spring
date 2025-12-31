package com.stolink.backend.domain.chapter.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateChapterRequest {
    private String title;
    private String content;
}
