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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
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
        log.info("[LikeService] toggleWorkLike called. userId: {}, workId: {}", userId, workId);
        AuthValidationUtil.requireUserId(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        Work work = workRepository.findById(workId)
                .orElseThrow(() -> new ResourceNotFoundException("작품을 찾을 수 없습니다: " + workId));

        // 최적화: 단건 조회 (Optional)
        Optional<WorkLike> existingLike = workLikeRepository.findByUserIdAndWorkId(userId, workId);

        boolean liked;
        if (existingLike.isPresent()) {
            log.info("[LikeService] Found existing like. Deleting. userId: {}", userId);
            workLikeRepository.delete(existingLike.get());
            work.removeLike(); // Count 감소
            liked = false;
        } else {
            log.info("[LikeService] Like NOT FOUND. Creating new. userId: {}", userId);
            WorkLike like = WorkLike.builder()
                    .user(user)
                    .work(work)
                    .build();
            try {
                workLikeRepository.save(like);
                work.addLike(); // Count 증가
                log.info("[LikeService] Like saved.");
                liked = true;
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // 동시성 이슈로 이미 저장된 경우
                log.info("[LikeService] Duplicate like detected. Considering as liked.");
                liked = true;
            }
        }

        // work는 Dirty Checking으로 자동 저장됨

        long likeCount = work.getLikeCount();
        log.info("[LikeService] Final likeCount: {}, isLiked: {}", likeCount, liked);
        return LikeResponse.of(liked, likeCount);
    }

    /**
     * 특정 작품의 좋아요 상태 조회
     */
    public LikeResponse getWorkLikeStatus(UUID userId, UUID workId) {
        Work work = workRepository.findById(workId)
                .orElseThrow(() -> new ResourceNotFoundException("작품을 찾을 수 없습니다: " + workId));

        boolean liked = userId != null && workLikeRepository.existsByUserIdAndWorkId(userId, workId);
        long likeCount = work.getLikeCount();
        log.info("[LikeService] getWorkLikeStatus. workId: {}, userId: {} -> liked: {}", workId, userId, liked);
        return LikeResponse.of(liked, likeCount);
    }
}
