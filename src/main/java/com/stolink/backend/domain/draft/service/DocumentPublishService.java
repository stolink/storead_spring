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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentPublishService {

    private final EntityManager entityManager;

    /**
     * 지정된 Document들을 게시 완료 상태로 변경
     * 
     * 참고: EntityManager 네이티브 쿼리에서 List<UUID>를 PostgreSQL uuid[]로 직접 바인딩하면
     * ClassCastException이 발생하므로, 문자열 기반 IN 절을 사용합니다.
     */
    @Transactional
    public void markAsPublished(List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return;
        }

        try {
            List<UUID> uuids = parseAndValidateUuids(documentIds);
            if (uuids.isEmpty())
                return;

            // 개별 UUID를 반복하며 업데이트 (안전한 방식)
            int updatedCount = 0;
            for (UUID uuid : uuids) {
                String sql = "UPDATE documents SET is_published = true WHERE id = :id";
                int affected = entityManager.createNativeQuery(sql)
                        .setParameter("id", uuid)
                        .executeUpdate();
                updatedCount += affected;
            }

            log.info("Stolink DB publication status updated. count={}, ids={}", updatedCount, documentIds);
        } catch (Exception e) {
            log.error("Failed to update Stolink documents status", e);
            throw new RuntimeException("Stolink DB update failed", e);
        }
    }

    /**
     * 지정된 Document들을 미배포 상태로 변경
     * 
     * 참고: EntityManager 네이티브 쿼리에서 List<UUID>를 PostgreSQL uuid[]로 직접 바인딩하면
     * ClassCastException이 발생하므로, 문자열 기반 IN 절을 사용합니다.
     */
    @Transactional
    public void markAsUnpublished(List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return;
        }

        try {
            List<UUID> uuids = parseAndValidateUuids(documentIds);
            if (uuids.isEmpty())
                return;

            // 개별 UUID를 반복하며 업데이트 (안전한 방식)
            int updatedCount = 0;
            for (UUID uuid : uuids) {
                String sql = "UPDATE documents SET is_published = false WHERE id = :id";
                int affected = entityManager.createNativeQuery(sql)
                        .setParameter("id", uuid)
                        .executeUpdate();
                updatedCount += affected;
            }

            log.info("Stolink DB publication status reverted. count={}, ids={}", updatedCount, documentIds);
        } catch (Exception e) {
            log.error("Failed to revert Stolink documents status", e);
            throw new RuntimeException("Stolink DB status revert failed", e);
        }
    }

    private List<UUID> parseAndValidateUuids(List<String> documentIds) {
        List<UUID> validUuids = new ArrayList<>();
        for (String docId : documentIds) {
            try {
                validUuids.add(UUID.fromString(docId));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid UUID format: {}", docId);
            }
        }
        return validUuids;
    }
}
