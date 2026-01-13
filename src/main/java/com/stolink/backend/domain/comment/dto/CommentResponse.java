package com.stolink.backend.domain.comment.dto;

import com.stolink.backend.domain.comment.entity.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CommentResponse {

    private UUID id;
    private UUID chapterId;
    private UUID userId;
    private String userNickname;
    private String userAvatarUrl;
    private UUID parentId;
    private String content;
    private String relationId;
    private Long likeCount;
    private int replyCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .chapterId(comment.getChapter().getId())
                .userId(comment.getUser().getId())
                .userNickname(comment.getUser().getNickname())
                .userAvatarUrl(comment.getUser().getAvatarUrl())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .content(comment.getContent())
                .relationId(comment.getRelationId())
                .likeCount(comment.getLikeCount())
                .replyCount(0)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    public static CommentResponse from(Comment comment, int replyCount) {
        return CommentResponse.builder()
                .id(comment.getId())
                .chapterId(comment.getChapter().getId())
                .userId(comment.getUser().getId())
                .userNickname(comment.getUser().getNickname())
                .userAvatarUrl(comment.getUser().getAvatarUrl())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .content(comment.getContent())
                .relationId(comment.getRelationId())
                .likeCount(comment.getLikeCount())
                .replyCount(replyCount)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
