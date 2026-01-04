package com.stolink.backend.domain.payment.dto.response;

import com.stolink.backend.domain.payment.entity.CreditTransaction;

import java.time.LocalDateTime;

public record CreditTransactionResponse(
    String id,
    String type,
    Long amount,
    Long balanceBefore,
    Long balanceAfter,
    String description,
    String referenceType,
    String referenceId,
    LocalDateTime createdAt
) {
    public static CreditTransactionResponse from(CreditTransaction tx) {
        return new CreditTransactionResponse(
            tx.getId().toString(),
            tx.getType().name(),
            tx.getAmount(),
            tx.getBalanceBefore(),
            tx.getBalanceAfter(),
            tx.getDescription(),
            tx.getReferenceType(),
            tx.getReferenceId(),
            tx.getCreatedAt()
        );
    }
}
