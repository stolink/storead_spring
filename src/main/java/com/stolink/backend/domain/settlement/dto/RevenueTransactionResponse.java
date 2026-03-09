package com.stolink.backend.domain.settlement.dto;

import com.stolink.backend.domain.settlement.entity.RevenueTransaction;
import com.stolink.backend.domain.settlement.entity.RevenueTransactionType;

import java.time.LocalDateTime;
import java.util.UUID;

public record RevenueTransactionResponse(
        UUID id,
        UUID workId,
        UUID chapterId,
        UUID buyerUserId,
        RevenueTransactionType type,
        Long creditAmount,
        Double platformFeeRate,
        Long platformFee,
        Long authorShare,
        UUID settlementId,
        LocalDateTime createdAt
) {
    public static RevenueTransactionResponse from(RevenueTransaction tx) {
        return new RevenueTransactionResponse(
                tx.getId(),
                tx.getWorkId(),
                tx.getChapterId(),
                tx.getBuyerUserId(),
                tx.getType(),
                tx.getCreditAmount(),
                tx.getPlatformFeeRate(),
                tx.getPlatformFee(),
                tx.getAuthorShare(),
                tx.getSettlementId(),
                tx.getCreatedAt()
        );
    }
}
