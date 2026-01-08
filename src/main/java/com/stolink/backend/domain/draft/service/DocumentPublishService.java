package com.stolink.backend.domain.draft.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Document 게시 상태 업데이트 서비스
 * 
 * Storead에서 게시 완료 시 Stolink의 documents 테이블의 is_published 필드를 업데이트
 * 같은 DB를 공유하므로 직접 접근 가능
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentPublishService {

    private final EntityManager entityManager;

    /**
     * 지정된 Document들을 게시 완료 상태로 변경
     * 
     * @param documentIds 게시 완료된 Document ID 목록
     * @throws IllegalArgumentException UUID 형식이 잘못된 경우
     */
    @Transactional
    public void markAsPublished(List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            log.warn("markAsPublished called with empty documentIds");
            return;
        }

        log.info("Attempting to update Stolink documents status to is_published=true for IDs: {}", documentIds);

        try {
            // 사전 검증: 모든 ID의 UUID 형식 확인
            List<UUID> uuids = parseAndValidateUuids(documentIds);

            if (uuids.isEmpty()) {
                log.warn("No valid UUIDs found in documentIds: {}", documentIds);
                return;
            }

            // PostgreSQL Native Query에서 IN (:ids) 방식보다 안전한 ANY 방식을 사용합니다.
            // 명시적 캐스팅(uuid[])을 추가하여 바인딩 오류를 방지합니다.
            String sql = "UPDATE documents SET is_published = true WHERE id = ANY(CAST(:ids AS uuid[]))";
            int updatedCount = entityManager.createNativeQuery(sql)
                    .setParameter("ids", uuids)
                    .executeUpdate();

            if (updatedCount == 0) {
                log.warn("Stolink DB update returned 0 rows affected. IDs might be missing in 'documents' table: {}",
                        documentIds);
            } else {
                log.info("Stolink DB publication status updated successfully. count={}, affected_ids={}", updatedCount,
                        documentIds);
            }
        } catch (Exception e) {
            log.error("Failed to update Stolink documents status: " + e.getMessage(), e);
            // 트랜잭션 전파를 위해 런타임 예외로 다시 던짐
            throw new RuntimeException("Stolink DB update failed", e);
        }
    }

    /**
     * 지정된 Document들을 미배포 상태로 변경 (게시 취소 시 사용)
     * 
     * @param documentIds 미배포로 변경할 Document ID 목록
     * @throws RuntimeException 상태 변경 실패 시 (트랜잭션 롤백을 위해 예외 전파)
     */
    @Transactional
    public void markAsUnpublished(List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return;
        }

        try {
            // 사전 검증: 모든 ID의 UUID 형식 확인
            List<UUID> uuids = parseAndValidateUuids(documentIds);

            if (uuids.isEmpty()) {
                log.warn("No valid UUIDs found in documentIds for unpublish: {}", documentIds);
                return;
            }

            // PostgreSQL Native Query에서 IN (:ids) 방식보다 안전한 ANY 방식을 사용합니다.
            String sql = "UPDATE documents SET is_published = false WHERE id = ANY(CAST(:ids AS uuid[]))";
            int updatedCount = entityManager.createNativeQuery(sql)
                    .setParameter("ids", uuids)
                    .executeUpdate();

            log.info("Stolink DB publication status reverted (is_published=false). count={}, ids={}", updatedCount,
                    documentIds);
        } catch (Exception e) {
            log.error("Failed to revert Stolink documents status: " + e.getMessage(), e);
            // 예외 삼킴 방지: 상위 트랜잭션에 전파하여 데이터 불일치 방지
            throw new RuntimeException("Stolink DB status revert failed", e);
        }
    }

    /**
     * 문자열 ID 목록을 UUID로 파싱 및 검증
     * 잘못된 형식의 ID는 경고 로그를 남기고 건너뜀
     * 
     * @param documentIds 문자열 형태의 UUID 목록
     * @return 유효한 UUID 목록
     */
    private List<UUID> parseAndValidateUuids(List<String> documentIds) {
        List<UUID> validUuids = new ArrayList<>();

        for (String docId : documentIds) {
            try {
                validUuids.add(UUID.fromString(docId));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid UUID format skipped: {}", docId);
            }
        }

        return validUuids;
    }
}
