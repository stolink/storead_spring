package com.stolink.backend.domain.settlement.entity;

public enum RevenueTransactionType {
    CHAPTER_SALE("챕터 판매"),
    REFUND("환불"),
    ADJUSTMENT("관리자 조정");

    private final String description;

    RevenueTransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
