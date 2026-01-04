package com.stolink.backend.domain.payment.entity;

public enum WebhookStatus {
    RECEIVED("수신됨"),
    PROCESSED("처리 완료"),
    FAILED("처리 실패"),
    DUPLICATE("중복 처리");

    private final String description;

    WebhookStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
