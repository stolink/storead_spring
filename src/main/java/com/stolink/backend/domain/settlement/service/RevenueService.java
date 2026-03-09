package com.stolink.backend.domain.settlement.service;

import com.stolink.backend.domain.chapter.entity.Chapter;
import com.stolink.backend.domain.chapter.entity.ChapterPurchase;
import com.stolink.backend.domain.chapter.repository.ChapterRepository;
import com.stolink.backend.domain.settlement.config.SettlementConfig;
import com.stolink.backend.domain.settlement.entity.AuthorRevenue;
import com.stolink.backend.domain.settlement.entity.RevenueTransaction;
import com.stolink.backend.domain.settlement.entity.RevenueTransactionType;
import com.stolink.backend.domain.settlement.repository.AuthorRevenueRepository;
import com.stolink.backend.domain.settlement.repository.RevenueTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevenueService {

    private final AuthorRevenueRepository authorRevenueRepository;
    private final RevenueTransactionRepository revenueTransactionRepository;
    private final ChapterRepository chapterRepository;
    private final SettlementConfig settlementConfig;

    /**
     * 챕터 판매 수익 기록
     * ChapterPurchaseService에서 구매 완료 후 호출
     */
    @Transactional
    public void recordChapterSaleRevenue(ChapterPurchase purchase) {
        Chapter chapter = chapterRepository.findById(purchase.getChapterId())
                .orElse(null);
        if (chapter == null) {
            log.warn("수익 기록 실패: 챕터를 찾을 수 없습니다. chapterId={}", purchase.getChapterId());
            return;
        }

        // 무료 챕터 - 수익 없음
        if (Boolean.TRUE.equals(chapter.getIsFree()) || purchase.getPricePaid() <= 0) {
            return;
        }

        UUID authorId = chapter.getWork().getAuthor().getId();

        // 작가 본인 구매 - 수익 미생성
        if (authorId.equals(purchase.getUserId())) {
            log.debug("작가 본인 구매 - 수익 미생성: authorId={}, chapterId={}", authorId, purchase.getChapterId());
            return;
        }

        // 중복 방지 (멱등성)
        if (revenueTransactionRepository.existsByPurchaseIdAndType(purchase.getId(), RevenueTransactionType.CHAPTER_SALE)) {
            log.debug("이미 기록된 판매 수익: purchaseId={}", purchase.getId());
            return;
        }

        double feeRate = settlementConfig.getPlatformFeeRate();
        RevenueTransaction tx = RevenueTransaction.createSaleTransaction(
                authorId,
                chapter.getWork().getId(),
                chapter.getId(),
                purchase.getUserId(),
                purchase.getId(),
                purchase.getPricePaid(),
                feeRate
        );

        // AuthorRevenue 갱신
        AuthorRevenue revenue = getOrCreateAuthorRevenue(authorId);
        revenue.addEarning(tx.getAuthorShare());

        try {
            revenueTransactionRepository.save(tx);
            log.info("챕터 판매 수익 기록: authorId={}, chapterId={}, authorShare={}",
                    authorId, chapter.getId(), tx.getAuthorShare());
        } catch (DataIntegrityViolationException e) {
            // 동시성으로 인한 중복 - 멱등하게 무시
            log.warn("중복 수익 기록 무시: purchaseId={}", purchase.getId());
        }
    }

    /**
     * 환불 수익 차감
     */
    @Transactional
    public void recordRefundRevenue(ChapterPurchase purchase) {
        Chapter chapter = chapterRepository.findById(purchase.getChapterId())
                .orElse(null);
        if (chapter == null) {
            log.warn("환불 수익 차감 실패: 챕터를 찾을 수 없습니다. chapterId={}", purchase.getChapterId());
            return;
        }

        UUID authorId = chapter.getWork().getAuthor().getId();

        // 중복 방지
        if (revenueTransactionRepository.existsByPurchaseIdAndType(purchase.getId(), RevenueTransactionType.REFUND)) {
            log.debug("이미 기록된 환불: purchaseId={}", purchase.getId());
            return;
        }

        double feeRate = settlementConfig.getPlatformFeeRate();
        RevenueTransaction tx = RevenueTransaction.createRefundTransaction(
                authorId,
                chapter.getWork().getId(),
                chapter.getId(),
                purchase.getUserId(),
                purchase.getId(),
                purchase.getPricePaid(),
                feeRate
        );

        AuthorRevenue revenue = authorRevenueRepository.findByAuthorIdWithLock(authorId)
                .orElseThrow(() -> new IllegalStateException("환불 대상 작가의 수익 정보가 없습니다."));

        revenue.deductEarning(Math.abs(tx.getAuthorShare()));
        revenueTransactionRepository.save(tx);

        log.info("환불 수익 차감: authorId={}, chapterId={}, deducted={}",
                authorId, chapter.getId(), Math.abs(tx.getAuthorShare()));
    }

    private AuthorRevenue getOrCreateAuthorRevenue(UUID authorId) {
        return authorRevenueRepository.findByAuthorIdWithLock(authorId)
                .orElseGet(() -> {
                    try {
                        return authorRevenueRepository.save(AuthorRevenue.createForAuthor(authorId));
                    } catch (DataIntegrityViolationException e) {
                        return authorRevenueRepository.findByAuthorIdWithLock(authorId)
                                .orElseThrow(() -> new IllegalStateException("작가 수익 정보 생성 실패"));
                    }
                });
    }
}
