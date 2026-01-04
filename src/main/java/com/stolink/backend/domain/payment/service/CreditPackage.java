package com.stolink.backend.domain.payment.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreditPackage {
    private Long id;
    private String name;
    private Long price;
    private Long creditAmount;
    private Long bonusCredit;
    private boolean isPopular;

    public Long getTotalCredit() {
        return creditAmount + bonusCredit;
    }
}
