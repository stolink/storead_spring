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
        log.info("[DocumentPublishService] markAsPublished called with documentIds={}", documentIds);

        if (documentIds == null || documentIds.isEmpty()) {
            log.warn("[DocumentPublishService] documentIds is null or empty, skipping update");
            return;
        }

        try {
            List<UUID> uuids = parseAndValidateUuids(documentIds);
            log.info("[DocumentPublishService] Parsed {} valid UUIDs from {} documentIds", uuids.size(),
                    documentIds.size());

            if (uuids.isEmpty()) {
                log.warn("[DocumentPublishService] No valid UUIDs after parsing, skipping update");
                return;
            }

            // 단일 벌크 업데이트 쿼리로 일괄 처리 (N+1 방지)
            // IN 절을 사용하여 JPA가 자동으로 List<UUID>를 처리하도록 합니다.
            String sql = "UPDATE documents SET is_published = true WHERE id IN (:ids)";
            int updatedCount = entityManager.createNativeQuery(sql)
                    .setParameter("ids", uuids)
                    .executeUpdate();

            log.info("[DocumentPublishService] BULK UPDATE result: affected={}", updatedCount);

            if (updatedCount == 0) {
                log.error(
                        "[DocumentPublishService] WARNING: No documents were updated! Check if documents exist in stolink DB.");
            }
        } catch (Exception e) {
            log.error("[DocumentPublishService] Failed to update Stolink documents status", e);
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
            if (uuids.isEmpty())
                return;

            String sql = "UPDATE documents SET is_published = false WHERE id IN (:ids)";
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
