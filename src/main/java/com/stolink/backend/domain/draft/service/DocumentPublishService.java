package com.stolink.backend.domain.draft.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
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
     */
    @Transactional
    public void markAsPublished(List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            log.warn("markAsPublished called with empty documentIds");
            return;
        }

        log.info("Attempting to update Stolink documents status to is_published=true for IDs: {}", documentIds);

        try {
            // Hibernate는 UUID[]를 PostgreSQL uuid[]로 직접 변환하지 못하므로
            // 각 UUID를 개별적으로 처리하거나 IN 절을 동적으로 생성해야 함
            int updatedCount = 0;
            for (String docId : documentIds) {
                try {
                    UUID uuid = UUID.fromString(docId);
                    String sql = "UPDATE documents SET is_published = true WHERE id = :id";
                    Query query = entityManager.createNativeQuery(sql);
                    query.setParameter("id", uuid);
                    updatedCount += query.executeUpdate();
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid UUID format: {}", docId);
                }
            }

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
            // Hibernate는 UUID[]를 PostgreSQL uuid[]로 직접 변환하지 못하므로
            // 각 UUID를 개별적으로 처리
            int updatedCount = 0;
            for (String docId : documentIds) {
                try {
                    UUID uuid = UUID.fromString(docId);
                    String sql = "UPDATE documents SET is_published = false WHERE id = :id";
                    Query query = entityManager.createNativeQuery(sql);
                    query.setParameter("id", uuid);
                    updatedCount += query.executeUpdate();
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid UUID format: {}", docId);
                }
            }
            log.info("Stolink DB publication status reverted (is_published=false). count={}, ids={}", updatedCount,
                    documentIds);
        } catch (Exception e) {
            log.error("Failed to revert Stolink documents status: " + e.getMessage(), e);
            // 예외 삼킴 방지: 상위 트랜잭션에 전파하여 데이터 불일치 방지
            throw new RuntimeException("Stolink DB status revert failed", e);
        }
    }
}
