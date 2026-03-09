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
import com.stolink.backend.domain.settlement.service.RevenueService;
import com.stolink.backend.global.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChapterPurchaseService {

    private final ChapterPurchaseRepository chapterPurchaseRepository;
    private final ChapterRepository chapterRepository;
    private final CreditService creditService;
    private final RevenueService revenueService;

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
     * - 결제 원자성 보장: 구매 기록 먼저 저장 후 크레딧 차감
     * - 중복 구매 방지: 유니크 제약 조건 활용
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

        // 2. 이미 구매했는지 확인 (멱등성 보장)
        if (chapterPurchaseRepository.existsByUserIdAndChapterId(userId, chapterId)) {
            return;
        }

        // 3. 구매 기록 먼저 저장 (유니크 제약 조건으로 중복 방지)
        // 크레딧 차감 전에 저장하여 결제 실패 시 롤백 보장
        ChapterPurchase purchase = ChapterPurchase.builder()
                .userId(userId)
                .chapterId(chapterId)
                .pricePaid(chapter.getPrice())
                .build();

        try {
            chapterPurchaseRepository.save(purchase);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 동시성 이슈로 이미 구매된 경우 (멱등성 보장)
            return;
        }

        // 4. 크레딧 차감 (트랜잭션 내에서 실행되므로 실패 시 구매 기록도 롤백됨)
        CreditUseRequest useRequest = new CreditUseRequest(
                (long) chapter.getPrice(),
                "챕터 구매: " + chapter.getTitle(),
                "CHAPTER",
                chapterId.toString());
        creditService.useCredit(userId, useRequest);

        // 5. 작가 수익 기록
        try {
            revenueService.recordChapterSaleRevenue(purchase);
        } catch (Exception e) {
            log.warn("수익 기록 실패 (구매는 정상 처리됨): purchaseId={}, error={}",
                    purchase.getId(), e.getMessage());
        }
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
