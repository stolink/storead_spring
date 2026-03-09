package com.stolink.backend.domain.settlement.entity;

public enum SettlementStatus {
    PENDING("정산 대기"),
    CONFIRMED("작가 확인 완료"),
    PROCESSING("정산 처리 중"),
    COMPLETED("정산 완료"),
    FAILED("정산 실패"),
    REJECTED("작가 거절");

    private final String description;

    SettlementStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
