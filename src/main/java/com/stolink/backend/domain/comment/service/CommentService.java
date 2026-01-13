package com.stolink.backend.domain.comment.service;

import com.stolink.backend.domain.chapter.entity.Chapter;
import com.stolink.backend.domain.chapter.repository.ChapterRepository;
import com.stolink.backend.domain.comment.dto.CommentResponse;
import com.stolink.backend.domain.comment.dto.CreateCommentRequest;
import com.stolink.backend.domain.comment.entity.Comment;
import com.stolink.backend.domain.comment.repository.CommentRepository;
import com.stolink.backend.domain.user.entity.User;
import com.stolink.backend.domain.user.repository.UserRepository;
import com.stolink.backend.global.common.exception.AccessDeniedException;
import com.stolink.backend.global.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

        private final CommentRepository commentRepository;
        private final ChapterRepository chapterRepository;
        private final UserRepository userRepository;

        public Page<CommentResponse> getComments(UUID chapterId, String relationId, Pageable pageable) {
                Page<Comment> comments;
                if (relationId != null && !relationId.isBlank()) {
                        comments = commentRepository.findByChapterIdAndParentIsNullAndRelationIdOrderByCreatedAtDesc(
                                        chapterId, relationId, pageable);
                } else {
                        comments = commentRepository
                                        .findByChapterIdAndParentIsNullAndRelationIdIsNullOrderByCreatedAtDesc(
                                                        chapterId, pageable);
                }
                return comments.map(comment -> {
                        int replyCount = commentRepository.countByParentId(comment.getId());
                        return CommentResponse.from(comment, replyCount);
                });
        }

        public List<CommentResponse> getReplies(UUID commentId) {
                return commentRepository.findByParentIdOrderByCreatedAtAsc(commentId)
                                .stream()
                                .map(comment -> CommentResponse.from(comment, 0))
                                .collect(Collectors.toList());
        }

        @Transactional
        public CommentResponse createComment(UUID userId, UUID chapterId, CreateCommentRequest request) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));

                Chapter chapter = chapterRepository.findById(chapterId)
                                .orElseThrow(() -> new ResourceNotFoundException("챕터를 찾을 수 없습니다: " + chapterId));

                Comment comment = Comment.builder()
                                .chapter(chapter)
                                .user(user)
                                .content(request.getContent())
                                .relationId(request.getRelationId())
                                .build();

                Comment saved = commentRepository.save(comment);
                return CommentResponse.from(saved);
        }

        @Transactional
        public CommentResponse createReply(UUID userId, UUID commentId, CreateCommentRequest request) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));

                Comment parent = commentRepository.findById(commentId)
                                .orElseThrow(() -> new ResourceNotFoundException("댓글을 찾을 수 없습니다: " + commentId));

                Comment reply = Comment.builder()
                                .chapter(parent.getChapter())
                                .user(user)
                                .parent(parent)
                                .content(request.getContent())
                                .build();

                Comment saved = commentRepository.save(reply);
                return CommentResponse.from(saved);
        }

        @Transactional
        public void deleteComment(UUID userId, UUID commentId) {
                Comment comment = commentRepository.findById(commentId)
                                .orElseThrow(() -> new ResourceNotFoundException("댓글을 찾을 수 없습니다: " + commentId));

                if (!comment.getUser().getId().equals(userId)) {
                        throw new AccessDeniedException("해당 댓글을 삭제할 권한이 없습니다");
                }

                // 답글이 있으면 답글도 삭제
                commentRepository.deleteByParentId(commentId);
                commentRepository.delete(comment);
        }
}
