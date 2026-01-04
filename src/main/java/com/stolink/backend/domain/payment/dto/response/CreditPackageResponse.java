package com.stolink.backend.domain.payment.dto.response;

public record CreditPackageResponse(
    Long id,
    String name,
    Long price,
    Long creditAmount,
    Long bonusCredit,
    boolean isPopular
) {
    public Long getTotalCredit() {
        return creditAmount + bonusCredit;
    }
}
