package com.stolink.backend.domain.chapter.repository;

import com.stolink.backend.domain.chapter.entity.ChapterPurchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChapterPurchaseRepository extends JpaRepository<ChapterPurchase, UUID> {

    // 특정 사용자의 특정 챕터 구매 여부 확인
    boolean existsByUserIdAndChapterId(UUID userId, UUID chapterId);

    // 사용자의 모든 구매 기록 조회 (최신순)
    List<ChapterPurchase> findAllByUserIdOrderByPurchasedAtDesc(UUID userId);
}
