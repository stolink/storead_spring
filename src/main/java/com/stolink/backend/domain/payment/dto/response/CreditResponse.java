package com.stolink.backend.domain.payment.dto.response;

import com.stolink.backend.domain.payment.entity.Credit;

import java.time.LocalDateTime;

public record CreditResponse(
    String id,
    Long balance,
    Long totalCharged,
    Long totalUsed,
    LocalDateTime updatedAt
) {
    public static CreditResponse from(Credit credit) {
        return new CreditResponse(
            credit.getId().toString(),
            credit.getBalance(),
            credit.getTotalCharged(),
            credit.getTotalUsed(),
            credit.getUpdatedAt()
        );
    }
}
