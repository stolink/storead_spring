package com.stolink.backend.domain.chapter.repository;

import com.stolink.backend.domain.chapter.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChapterRepository extends JpaRepository<Chapter, UUID> {

        List<Chapter> findByWorkIdOrderByChapterNumberAsc(UUID workId);

        Optional<Chapter> findByIdAndWorkId(UUID id, UUID workId);

        int countByWorkId(UUID workId);

        @Query("SELECT MAX(c.chapterNumber) FROM Chapter c WHERE c.work.id = :workId")
        Optional<Integer> findMaxChapterNumberByWorkId(@Param("workId") UUID workId);

        @Query("SELECT c FROM Chapter c WHERE c.work.id = :workId AND c.chapterNumber >= :chapterNumber ORDER BY c.chapterNumber ASC")
        List<Chapter> findByWorkIdAndChapterNumberGreaterThanEqual(
                        @Param("workId") UUID workId,
                        @Param("chapterNumber") int chapterNumber);

        @Query("SELECT c FROM Chapter c WHERE c.work.id = :workId AND c.chapterNumber > :chapterNumber ORDER BY c.chapterNumber ASC")
        List<Chapter> findByWorkIdAndChapterNumberGreaterThan(
                        @Param("workId") UUID workId,
                        @Param("chapterNumber") int chapterNumber);

        /**
         * 이전 챕터 ID 조회 (현재 챕터보다 작은 번호 중 가장 큰 것)
         */
        @Query("SELECT c.id FROM Chapter c WHERE c.work.id = :workId AND c.chapterNumber < :currentNumber ORDER BY c.chapterNumber DESC LIMIT 1")
        Optional<UUID> findPrevChapterId(@Param("workId") UUID workId, @Param("currentNumber") int currentNumber);

        /**
         * 다음 챕터 ID 조회 (현재 챕터보다 큰 번호 중 가장 작은 것)
         */
        @Query("SELECT c.id FROM Chapter c WHERE c.work.id = :workId AND c.chapterNumber > :currentNumber ORDER BY c.chapterNumber ASC LIMIT 1")
        Optional<UUID> findNextChapterId(@Param("workId") UUID workId, @Param("currentNumber") int currentNumber);

        void deleteByWorkId(UUID workId);

        /**
         * 시나리오 A, B용: 단일 documentId 필드에서 중복 체크
         */
        boolean existsByWorkIdAndDocumentId(UUID workId, String documentId);

        /**
         * 시나리오 C용: documentIds JSONB 배열에서 중복 체크
         * 특정 Work 내에 주어진 documentId가 포함된 Chapter가 존재하는지 확인
         */
        @Query(value = "SELECT EXISTS(" +
                        "SELECT 1 FROM chapters c " +
                        "WHERE c.work_id = :workId " +
                        "AND c.document_ids @> jsonb_build_array(CAST(:documentId AS text))" +
                        ")", nativeQuery = true)
        boolean existsByWorkIdAndDocumentIdInArray(@Param("workId") UUID workId,
                        @Param("documentId") String documentId);

        /**
         * 일괄 중복 체크: 여러 documentId를 한 번의 쿼리로 검증 (N+1 방지)
         * 
         * PostgreSQL 연산자(?) 대신 함수(jsonb_exists_any)를 사용하여 
         * Spring Data JPA 쿼리 파싱 충돌(502 Bad Gateway 원인)을 원천 차단
         */
        @Query(value = "SELECT EXISTS(" +
                        "SELECT 1 FROM chapters c " +
                        "WHERE c.work_id = :workId " +
                        "AND (c.document_id IN (:docIds) " +
                        "OR jsonb_exists_any(c.document_ids, CAST(:docIds AS text[])))" +
                        ")", nativeQuery = true)
        boolean existsByWorkIdAndAnyDocumentIds(@Param("workId") UUID workId,
                        @Param("docIds") List<String> docIds);

        /**
         * 일괄 중복 체크: 중복된 문서 ID 목록 반환 (에러 메시지용)
         */
        @Query(value = "SELECT DISTINCT d.doc_id FROM (" +
                        "SELECT c.document_id AS doc_id FROM chapters c " +
                        "WHERE c.work_id = :workId AND c.document_id IN (:docIds) " +
                        "UNION " +
                        "SELECT jsonb_array_elements_text(c.document_ids) AS doc_id FROM chapters c " +
                        "WHERE c.work_id = :workId AND jsonb_exists_any(c.document_ids, CAST(:docIds AS text[]))" +
                        ") d WHERE d.doc_id IN (:docIds)", nativeQuery = true)
        List<String> findDuplicateDocumentIds(@Param("workId") UUID workId,
                        @Param("docIds") List<String> docIds);
}
