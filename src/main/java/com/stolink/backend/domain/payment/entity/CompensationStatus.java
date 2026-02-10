package com.stolink.backend.domain.payment.entity;

public enum CompensationStatus {
    PENDING("보상 대기"),
    RESOLVED("보상 완료"),
    REQUIRES_MANUAL("수동 처리 필요");

    private final String description;

    CompensationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
