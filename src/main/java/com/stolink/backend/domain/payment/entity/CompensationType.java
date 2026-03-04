package com.stolink.backend.domain.payment.entity;

public enum CompensationType {
    CANCEL_CREDIT_DEDUCTION("결제 취소 후 크레딧 차감 실패");

    private final String description;

    CompensationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
