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

        // 참고: existsByWorkIdAndAnyDocumentIds, findDuplicateDocumentIds 메서드는
        // Spring Data JPA가 List<String>을 PostgreSQL text[]로 변환할 수 없어 500 에러가 발생했습니다.
        // 해당 로직은 ChapterDuplicationChecker 서비스로 이동되었습니다.
}
