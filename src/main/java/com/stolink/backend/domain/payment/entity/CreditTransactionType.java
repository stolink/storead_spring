package com.stolink.backend.domain.payment.entity;

public enum CreditTransactionType {
    CHARGE("충전"),
    USE("사용"),
    REFUND("환불"),
    EXPIRE("만기 소멸"),
    ADMIN_ADJUST("관리자 조정");

    private final String description;

    CreditTransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
