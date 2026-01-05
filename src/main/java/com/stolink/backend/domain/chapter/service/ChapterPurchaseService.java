package com.stolink.backend.domain.chapter.service;

import com.stolink.backend.domain.chapter.dto.PurchaseCheckResponse;
import com.stolink.backend.domain.chapter.entity.Chapter;
import com.stolink.backend.domain.chapter.entity.ChapterPurchase;
import com.stolink.backend.domain.chapter.repository.ChapterPurchaseRepository;
import com.stolink.backend.domain.chapter.repository.ChapterRepository;
import com.stolink.backend.domain.payment.dto.request.CreditUseRequest;
import com.stolink.backend.domain.payment.dto.response.CreditCheckResponse;
import com.stolink.backend.domain.payment.dto.response.CreditResponse;
import com.stolink.backend.domain.payment.service.CreditService;
import com.stolink.backend.global.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChapterPurchaseService {

    private final ChapterPurchaseRepository chapterPurchaseRepository;
    private final ChapterRepository chapterRepository;
    private final CreditService creditService;

    /**
     * 챕터 구매 가능 여부 확인
     */
    @Transactional(readOnly = true)
    public PurchaseCheckResponse checkPurchaseAvailability(UUID userId, UUID chapterId) {
        // 1. 챕터 조회
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));

        // 유료가 아니면 무조건 가능
        if (Boolean.TRUE.equals(chapter.getIsFree())) {
            return PurchaseCheckResponse.of(true, 0L, 0, false);
        }

        // 2. 이미 구매했는지 확인
        boolean alreadyPurchased = chapterPurchaseRepository.existsByUserIdAndChapterId(userId, chapterId);
        if (alreadyPurchased) {
            CreditResponse credit = creditService.getCredit(userId);
            // Record 접근자 사용
            return PurchaseCheckResponse.of(true, credit.balance(), chapter.getPrice(), true);
        }

        // 3. 잔액 확인
        CreditCheckResponse creditCheck = creditService.checkCredit(userId, (long) chapter.getPrice());

        // Record 접근자 사용
        return PurchaseCheckResponse.of(
                creditCheck.available(),
                creditCheck.currentBalance(),
                chapter.getPrice(),
                false);
    }

    /**
     * 챕터 구매 실행
     */
    @Transactional
    public void purchaseChapter(UUID userId, UUID chapterId) {
        // 1. 챕터 조회
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));

        // 무료 챕터는 구매 불필요
        if (Boolean.TRUE.equals(chapter.getIsFree())) {
            return;
        }

        // 2. 이미 구매했는지 확인
        if (chapterPurchaseRepository.existsByUserIdAndChapterId(userId, chapterId)) {
            return;
        }

        // 3. 크레딧 차감
        CreditUseRequest useRequest = new CreditUseRequest(
                (long) chapter.getPrice(),
                "챕터 구매: " + chapter.getTitle(),
                "CHAPTER",
                chapterId.toString());
        creditService.useCredit(userId, useRequest);

        // 4. 구매 기록 저장
        ChapterPurchase purchase = ChapterPurchase.builder()
                .userId(userId)
                .chapterId(chapterId)
                .pricePaid(chapter.getPrice())
                .build();
        chapterPurchaseRepository.save(purchase);
    }

    /**
     * 챕터 접근 권한 확인
     */
    @Transactional(readOnly = true)
    public boolean hasAccess(UUID userId, UUID chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));

        if (Boolean.TRUE.equals(chapter.getIsFree())) {
            return true;
        }

        return chapterPurchaseRepository.existsByUserIdAndChapterId(userId, chapterId);
    }
}
