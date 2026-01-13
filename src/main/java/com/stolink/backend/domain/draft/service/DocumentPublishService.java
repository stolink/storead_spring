package com.stolink.backend.domain.draft.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stolink.backend.domain.document.repository.DocumentRepository;
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

    private final DocumentRepository documentRepository;
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

            // IN 절을 사용한 벌크 업데이트 (Hibernate 호환)
            // UUID 리터럴 리스트를 직접 SQL에 삽입 (Prepared Statement 파라미터 바인딩 이슈 회피)
            String uuidList = uuids.stream()
                    .map(uuid -> "'" + uuid.toString() + "'")
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");

            String sql = "UPDATE documents SET is_published = true WHERE id IN (" + uuidList + ")";
            int updatedCount = entityManager.createNativeQuery(sql)
                    .executeUpdate();

            log.info("[DocumentPublishService] BULK UPDATE completed for {} documents", uuids.size());
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

            // IN 절을 사용한 벌크 업데이트 (Hibernate 호환)
            String uuidList = uuids.stream()
                    .map(uuid -> "'" + uuid.toString() + "'")
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");

            String sql = "UPDATE documents SET is_published = false WHERE id IN (" + uuidList + ")";
            int updatedCount = entityManager.createNativeQuery(sql)
                    .executeUpdate();

            log.info("Stolink DB publication status reverted. count={}, ids={}", uuids.size(), documentIds);
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
