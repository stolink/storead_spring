package com.stolink.backend.domain.like.service;

import com.stolink.backend.domain.comment.entity.Comment;
import com.stolink.backend.domain.comment.repository.CommentRepository;
import com.stolink.backend.domain.like.dto.LikeResponse;
import com.stolink.backend.domain.like.entity.CommentLike;
import com.stolink.backend.domain.like.entity.WorkLike;
import com.stolink.backend.domain.like.repository.CommentLikeRepository;
import com.stolink.backend.domain.like.repository.WorkLikeRepository;
import com.stolink.backend.domain.user.entity.User;
import com.stolink.backend.domain.user.repository.UserRepository;
import com.stolink.backend.domain.work.entity.Work;
import com.stolink.backend.domain.work.repository.WorkRepository;
import com.stolink.backend.global.common.exception.ResourceNotFoundException;
import com.stolink.backend.global.util.AuthValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {

    private final CommentLikeRepository commentLikeRepository;
    private final CommentRepository commentRepository;
    private final WorkLikeRepository workLikeRepository;
    private final WorkRepository workRepository;
    private final UserRepository userRepository;

    /**
     * 댓글 좋아요 토글 (중복 방지)
     * - 이미 좋아요 → 삭제 + like_count 감소
     * - 좋아요 없음 → 생성 + like_count 증가
     */
    @Transactional
    public LikeResponse toggleCommentLike(UUID userId, UUID commentId) {
        AuthValidationUtil.requireUserId(userId);
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

    /**
     * 작품 좋아요 토글 (중복 방지)
     * - 이미 좋아요 → 삭제
     * - 좋아요 없음 → 생성
     */
    @Transactional
    public LikeResponse toggleWorkLike(UUID userId, UUID workId) {
        System.out.println("[LikeService] toggleWorkLike called. userId: " + userId + ", workId: " + workId);
        AuthValidationUtil.requireUserId(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        Work work = workRepository.findById(workId)
                .orElseThrow(() -> new ResourceNotFoundException("작품을 찾을 수 없습니다: " + workId));

        // 중복 방지 로직 강화: 리스트로 조회하여 모두 삭제
        List<WorkLike> existingLikes = workLikeRepository.findAllByUserIdAndWorkId(userId, workId);

        boolean liked;
        if (!existingLikes.isEmpty()) {
            System.out.println("[LikeService] Found " + existingLikes.size()
                    + " existing like(s). Deleting all. userId: " + userId);
            workLikeRepository.deleteAll(existingLikes);
            liked = false;
        } else {
            System.out.println("[LikeService] Like NOT FOUND. Creating new. userId: " + userId);
            WorkLike like = WorkLike.builder()
                    .user(user)
                    .work(work)
                    .build();
            workLikeRepository.save(like);
            System.out.println("[LikeService] Like saved.");
            liked = true;
        }

        long likeCount = workLikeRepository.countByWorkId(workId);
        System.out.println("[LikeService] Final likeCount: " + likeCount + ", isLiked: " + liked);
        return LikeResponse.of(liked, likeCount);
    }

    /**
     * 특정 작품의 좋아요 상태 조회
     */
    public LikeResponse getWorkLikeStatus(UUID userId, UUID workId) {
        if (!workRepository.existsById(workId)) {
            throw new ResourceNotFoundException("작품을 찾을 수 없습니다: " + workId);
        }

        boolean liked = userId != null && workLikeRepository.existsByUserIdAndWorkId(userId, workId);
        long likeCount = workLikeRepository.countByWorkId(workId);
        System.out.println(
                "[LikeService] getWorkLikeStatus. workId: " + workId + ", userId: " + userId + " -> liked: " + liked);
        return LikeResponse.of(liked, likeCount);
    }
}
