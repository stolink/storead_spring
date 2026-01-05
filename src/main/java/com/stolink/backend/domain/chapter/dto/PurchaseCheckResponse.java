package com.stolink.backend.domain.chapter.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PurchaseCheckResponse {
    private boolean canPurchase;
    private Long currentBalance;
    private Integer chapterPrice;
    private boolean alreadyPurchased;

    public static PurchaseCheckResponse of(boolean canPurchase, Long currentBalance, Integer chapterPrice,
            boolean alreadyPurchased) {
        return PurchaseCheckResponse.builder()
                .canPurchase(canPurchase)
                .currentBalance(currentBalance)
                .chapterPrice(chapterPrice)
                .alreadyPurchased(alreadyPurchased)
                .build();
    }
}
