package com.stolink.backend.domain.like.service;

import com.stolink.backend.domain.comment.entity.Comment;
import com.stolink.backend.domain.comment.repository.CommentRepository;
import com.stolink.backend.domain.like.dto.LikeResponse;
import com.stolink.backend.domain.like.entity.CommentLike;
import com.stolink.backend.domain.like.repository.CommentLikeRepository;
import com.stolink.backend.domain.user.entity.User;
import com.stolink.backend.domain.user.repository.UserRepository;
import com.stolink.backend.global.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {

    private final CommentLikeRepository commentLikeRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    /**
     * 댓글 좋아요 토글 (중복 방지)
     * - 이미 좋아요 → 삭제 + like_count 감소
     * - 좋아요 없음 → 생성 + like_count 증가
     */
    @Transactional
    public LikeResponse toggleCommentLike(UUID userId, UUID commentId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("댓글을 찾을 수 없습니다: " + commentId));

        Optional<CommentLike> existingLike = commentLikeRepository.findByCommentIdAndUserId(commentId, userId);

        boolean liked;
        if (existingLike.isPresent()) {
            commentLikeRepository.delete(existingLike.get());
            comment.decrementLikeCount();
            liked = false;
        } else {
            CommentLike like = CommentLike.builder()
                    .comment(comment)
                    .user(user)
                    .build();
            commentLikeRepository.save(like);
            comment.incrementLikeCount();
            liked = true;
        }

        return LikeResponse.of(liked, comment.getLikeCount());
    }
}
