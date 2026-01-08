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
     */
    @Transactional
    public void markAsPublished(List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return;
        }

        try {
            List<UUID> uuids = parseAndValidateUuids(documentIds);
            if (uuids.isEmpty()) return;

            // ANY 구문을 사용하여 안정적인 벌크 업데이트 수행 (PostgreSQL 전용)
            String sql = "UPDATE documents SET is_published = true WHERE id = ANY(CAST(:ids AS uuid[]))";
            int updatedCount = entityManager.createNativeQuery(sql)
                    .setParameter("ids", uuids)
                    .executeUpdate();

            log.info("Stolink DB publication status updated. count={}, ids={}", updatedCount, documentIds);
        } catch (Exception e) {
            log.error("Failed to update Stolink documents status", e);
            throw new RuntimeException("Stolink DB update failed", e);
        }
    }

    /**
     * 지정된 Document들을 미배포 상태로 변경
     */
    @Transactional
    public void markAsUnpublished(List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return;
        }

        try {
            List<UUID> uuids = parseAndValidateUuids(documentIds);
            if (uuids.isEmpty()) return;

            String sql = "UPDATE documents SET is_published = false WHERE id = ANY(CAST(:ids AS uuid[]))";
            int updatedCount = entityManager.createNativeQuery(sql)
                    .setParameter("ids", uuids)
                    .executeUpdate();

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
