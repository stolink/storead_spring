package com.stolink.backend.domain.like.entity;

import com.stolink.backend.domain.chapter.entity.Chapter;
import com.stolink.backend.domain.user.entity.User;
import com.stolink.backend.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * 챕터 좋아요 엔티티
 * 사용자가 특정 챕터에 좋아요를 누른 정보를 저장합니다.
 */
@Entity
@Table(name = "chapter_likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "chapter_id", "user_id" })
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ChapterLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
