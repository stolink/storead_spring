package com.stolink.backend.domain.comment.dto;

import com.stolink.backend.domain.comment.entity.Comment;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID chapterId,
        UUID userId,
        String userNickname,
        String userAvatarUrl,
        UUID parentId,
        String content,
        String relationId,
        Long likeCount,
        int replyCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static CommentResponse from(Comment comment) {
        return from(comment, 0);
    }

    public static CommentResponse from(Comment comment, int replyCount) {
        return new CommentResponse(
                comment.getId(),
                comment.getChapter().getId(),
                comment.getUser().getId(),
                comment.getUser().getNickname(),
                comment.getUser().getAvatarUrl(),
                comment.getParent() != null ? comment.getParent().getId() : null,
                comment.getContent(),
                comment.getRelationId(),
                comment.getLikeCount(),
                replyCount,
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }
}
