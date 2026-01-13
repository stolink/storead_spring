package com.stolink.backend.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCommentRequest(
        @NotBlank(message = "댓글 내용은 필수입니다") String content,
        String relationId) {
}
