package com.stolink.backend.domain.chapter.entity;

import com.stolink.backend.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 챕터 구매 이력 엔티티
 * 
 * 사용자가 특정 챕터를 구매한 기록을 저장합니다.
 * - 어떤 사용자가 (userId)
 * - 어떤 챕터를 (chapterId)
 * - 얼마에 (pricePaid)
 * - 언제 (purchasedAt -> BaseEntity.createdAt으로 대체 가능하나 명시적 필드 사용 고려)
 * 
 * 참고: User, Chapter와 직접적인 연관관계(FK)를 맺지 않고 ID만 저장하여
 * 느슨한 결합을 유지합니다 (MSA 전환 용이성 및 조회 성능 고려).
 */
@Entity
@Table(name = "chapter_purchases", indexes = {
        @Index(name = "idx_chapter_purchase_user", columnList = "user_id"),
        @Index(name = "idx_chapter_purchase_chapter", columnList = "chapter_id"),
        @Index(name = "idx_chapter_purchase_user_chapter", columnList = "user_id, chapter_id", unique = true)
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ChapterPurchase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "chapter_id", nullable = false)
    private UUID chapterId;

    @Column(name = "price_paid", nullable = false)
    private Integer pricePaid;

    @Column(name = "purchased_at", nullable = false)
    @Builder.Default
    private LocalDateTime purchasedAt = LocalDateTime.now();
}
