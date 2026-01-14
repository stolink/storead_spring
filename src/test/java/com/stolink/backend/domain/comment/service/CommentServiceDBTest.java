package com.stolink.backend.domain.comment.service;

import com.stolink.backend.domain.chapter.entity.Chapter;
import com.stolink.backend.domain.chapter.repository.ChapterRepository;
import com.stolink.backend.domain.comment.dto.CommentResponse;
import com.stolink.backend.domain.comment.dto.CreateCommentRequest;
import com.stolink.backend.domain.comment.entity.Comment;
import com.stolink.backend.domain.comment.repository.CommentRepository;
import com.stolink.backend.domain.user.entity.User;
import com.stolink.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
public class CommentServiceDBTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @Transactional
    void verifyParentIdPersistence() {
        // 1. Setup: Get existing User and Chapter
        User user = userRepository.findAll().stream().findFirst().orElseThrow();
        Chapter chapter = chapterRepository.findAll().stream().findFirst().orElseThrow();

        // 2. Create Parent Comment
        Comment parent = Comment.builder()
                .chapter(chapter)
                .user(user)
                .content("Parent Comment")
                .relationId("test-relation")
                .build();
        parent = commentRepository.saveAndFlush(parent);
        UUID parentId = parent.getId();

        // 3. Action: Create Reply via Service
        // Record는 생성자에서 모든 필드를 받음 (content, relationId)
        // Reply는 부모의 relationId를 상속받으므로 null로 전달하거나 "test-relation" 전달
        CreateCommentRequest request = new CreateCommentRequest("Reply Content", null);

        CommentResponse response = commentService.createReply(user.getId(), parentId, request);
        // Record는 getter 대신 필드명() 메서드 사용
        UUID replyId = response.id();

        // 4. Verification: Check the database directly (via Repository)
        Comment savedReply = commentRepository.findById(replyId).orElseThrow();

        // 핵심 검증 1: parentId가 정확히 저장되었는가? (Threaded 구조의 핵심)
        assertThat(savedReply.getParent()).isNotNull();
        assertThat(savedReply.getParent().getId()).isEqualTo(parentId);

        // 핵심 검증 2: relationId가 부모로부터 상속되었는가? (관계도 필터링 유지)
        assertThat(savedReply.getRelationId()).isEqualTo("test-relation");

        System.out.println("=== DB Verification Successful ===");
        System.out.println("Parent ID: " + parentId);
        System.out.println("Reply ID: " + replyId);
        System.out.println("Saved Parent ID in Reply: " + savedReply.getParent().getId());
    }
}
