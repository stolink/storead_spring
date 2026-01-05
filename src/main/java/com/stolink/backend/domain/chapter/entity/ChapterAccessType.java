package com.stolink.backend.domain.chapter.entity;

/**
 * 챕터 접근 유형 Enum
 * 
 * - FREE: 무료 챕터, 누구나 열람 가능
 * - PAID: 유료 챕터, 크레딧 결제 후 열람 가능
 * - EXCLUSIVE: 독점 챕터, 특별 결제 필요 (프리미엄)
 */
public enum ChapterAccessType {
    FREE("무료"),
    PAID("유료"),
    EXCLUSIVE("독점");

    private final String label;

    ChapterAccessType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
