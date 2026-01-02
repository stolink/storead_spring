package com.stolink.backend.domain.like.entity;

import com.stolink.backend.domain.user.entity.User;
import com.stolink.backend.domain.work.entity.Work;
import com.stolink.backend.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * 작품 좋아요 엔티티
 * - 유저와 작품 간의 Many-to-Many 관계 (중간 테이블)
 * - 한 유저는 하나의 작품에 한 번만 좋아요 가능 (user_id + work_id UNIQUE)
 */
@Entity
@Table(name = "work_likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "work_id" })
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WorkLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_id", nullable = false)
    private Work work;
}
