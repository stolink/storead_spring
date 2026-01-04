package com.stolink.backend.domain.payment.entity;

public enum PaymentStatus {
    PENDING("결제 대기"),
    READY("결제창 호출됨"),
    IN_PROGRESS("결제 진행 중"),
    DONE("결제 완료"),
    CANCELED("전체 취소"),
    PARTIAL_CANCELED("부분 취소"),
    FAILED("결제 실패"),
    EXPIRED("만료됨");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == DONE || this == CANCELED || this == FAILED || this == EXPIRED;
    }

    public boolean isCancelable() {
        return this == DONE || this == PARTIAL_CANCELED;
    }
}
