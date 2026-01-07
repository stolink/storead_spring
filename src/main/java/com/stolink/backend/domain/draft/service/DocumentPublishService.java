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
            // String UUID를 UUID 배열로 변환 (PostgreSQL ANY() 함수용)
            UUID[] uuidArray = documentIds.stream()
                    .map(UUID::fromString)
                    .toArray(UUID[]::new);

            // PostgreSQL ANY() 문법을 사용하여 IN 절 파라미터 바인딩 이슈 방지
            String sql = "UPDATE documents SET is_published = true WHERE id = ANY(:ids)";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("ids", uuidArray);

            int updatedCount = query.executeUpdate();

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
            // PostgreSQL ANY() 문법을 사용하여 IN 절 파라미터 바인딩 이슈 방지
            String sql = "UPDATE documents SET is_published = false WHERE id = ANY(:ids)";
            Query query = entityManager.createNativeQuery(sql);

            UUID[] uuidArray = documentIds.stream()
                    .map(UUID::fromString)
                    .toArray(UUID[]::new);

            query.setParameter("ids", uuidArray);
            int updatedCount = query.executeUpdate();
            log.info("Stolink DB publication status reverted (is_published=false). count={}, ids={}", updatedCount,
                    documentIds);
        } catch (Exception e) {
            log.error("Failed to revert Stolink documents status: " + e.getMessage(), e);
            // 예외 삼킴 방지: 상위 트랜잭션에 전파하여 데이터 불일치 방지
            throw new RuntimeException("Stolink DB status revert failed", e);
        }
    }
}
