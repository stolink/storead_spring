package com.stolink.backend.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateCommentRequest {

    @NotBlank(message = "댓글 내용은 필수입니다")
    private String content;

    private String relationId;
}
