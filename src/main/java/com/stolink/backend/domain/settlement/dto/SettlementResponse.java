package com.stolink.backend.domain.settlement.dto;

import com.stolink.backend.domain.settlement.entity.Settlement;
import com.stolink.backend.domain.settlement.entity.SettlementStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record SettlementResponse(
        UUID id,
        LocalDate periodStart,
        LocalDate periodEnd,
        Long grossAmount,
        Long platformFeeTotal,
        Long netAmount,
        Integer transactionCount,
        SettlementStatus status,
        String rejectReason,
        LocalDateTime confirmedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {
    public static SettlementResponse from(Settlement s) {
        return new SettlementResponse(
                s.getId(),
                s.getPeriodStart(),
                s.getPeriodEnd(),
                s.getGrossAmount(),
                s.getPlatformFeeTotal(),
                s.getNetAmount(),
                s.getTransactionCount(),
                s.getStatus(),
                s.getRejectReason(),
                s.getConfirmedAt(),
                s.getCompletedAt(),
                s.getCreatedAt()
        );
    }
}
