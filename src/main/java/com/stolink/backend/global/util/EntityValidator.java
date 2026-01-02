package com.stolink.backend.global.util;

import com.stolink.backend.global.common.exception.ResourceNotFoundException;

import java.util.UUID;

/**
 * 엔티티 관련 검증 유틸리티 클래스
 * - findById 호출 전 ID null 체크를 수행하여 명확한 에러 메시지 제공
 */
public final class EntityValidator {

    private EntityValidator() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }

    /**
     * UUID가 null인지 검증합니다.
     * null인 경우 ResourceNotFoundException을 던집니다.
     *
     * @param id        검증할 UUID
     * @param fieldName 필드명 (에러 메시지에 표시)
     * @throws ResourceNotFoundException UUID가 null인 경우
     */
    public static void requireNonNull(UUID id, String fieldName) {
        if (id == null) {
            throw new ResourceNotFoundException(fieldName + "는(은) null일 수 없습니다.");
        }
    }

    /**
     * UUID 문자열이 유효한 형식인지 검증합니다.
     *
     * @param uuidString 검증할 UUID 문자열
     * @param fieldName  필드명 (에러 메시지에 표시)
     * @return 유효한 UUID 객체
     * @throws ResourceNotFoundException 유효하지 않은 UUID 형식인 경우
     */
    public static UUID parseUUID(String uuidString, String fieldName) {
        if (uuidString == null || uuidString.isBlank()) {
            throw new ResourceNotFoundException(fieldName + "는(은) 비어있을 수 없습니다.");
        }
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException(fieldName + "의 형식이 올바르지 않습니다: " + uuidString);
        }
    }
}
