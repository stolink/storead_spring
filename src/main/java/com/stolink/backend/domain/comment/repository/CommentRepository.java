package com.stolink.backend.domain.comment.repository;

import com.stolink.backend.domain.comment.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    // 최상위 댓글만 조회 (parent가 null)
    // 최상위 댓글 조회 (relationId가 없는 경우 - 챕터 전체 댓글)
    Page<Comment> findByChapterIdAndParentIsNullAndRelationIdIsNullOrderByCreatedAtDesc(UUID chapterId,
            Pageable pageable);

    // 최상위 댓글 조회 (relationId가 있는 경우 - 특정 관계 댓글)
    Page<Comment> findByChapterIdAndParentIsNullAndRelationIdOrderByCreatedAtDesc(UUID chapterId, String relationId,
            Pageable pageable);

    // 특정 댓글의 답글 조회
    List<Comment> findByParentIdOrderByCreatedAtAsc(UUID parentId);

    // 특정 댓글의 답글 수
    int countByParentId(UUID parentId);

    // 특정 챕터의 댓글 수
    int countByChapterId(UUID chapterId);

    // 특정 댓글의 모든 답글 삭제
    void deleteByParentId(UUID parentId);
}
