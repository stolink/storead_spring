package com.stolink.backend.domain.work.repository;

import com.stolink.backend.domain.work.entity.Work;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkRepository
                extends JpaRepository<Work, UUID>,
                org.springframework.data.jpa.repository.JpaSpecificationExecutor<Work> {

        @Override
        @EntityGraph(attributePaths = { "author" })
        Page<Work> findAll(Pageable pageable);

        // Specification을 사용하는 findAll에도 @EntityGraph 적용 (LazyInitializationException
        // 방지)
        @Override
        @EntityGraph(attributePaths = { "author" })
        Page<Work> findAll(org.springframework.data.jpa.domain.Specification<Work> spec, Pageable pageable);

        @EntityGraph(attributePaths = { "author" })
        Page<Work> findByAuthorId(UUID authorId, Pageable pageable);

        Optional<Work> findByIdAndAuthorId(UUID id, UUID authorId);

        /**
         * 키워드로 작품 검색 (N+1 문제 해결 - @EntityGraph 사용)
         */
        @EntityGraph(attributePaths = { "author" })
        @Query("SELECT w FROM Work w WHERE w.title LIKE %:keyword% OR w.author.nickname LIKE %:keyword%")
        Page<Work> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

        /**
         * 작품 ID 목록으로 챕터 수 조회 (N+1 문제 해결 - 배치 조회)
         */
        @Query("SELECT c.work.id, COUNT(c) FROM Chapter c WHERE c.work.id IN :workIds GROUP BY c.work.id")
        List<Object[]> countChaptersByWorkIds(@Param("workIds") List<UUID> workIds);

        @EntityGraph(attributePaths = { "author" })
        @Query("SELECT w FROM Work w ORDER BY w.createdAt DESC")
        Page<Work> findAllOrderByCreatedAtDesc(Pageable pageable);

        /**
         * projectId로 작품 조회 (Stolink 프로젝트 매핑용)
         */
        @EntityGraph(attributePaths = { "author" })
        Optional<Work> findByProjectId(String projectId);

        /**
         * 장르별 작품 조회
         */
        @EntityGraph(attributePaths = { "author" })
        Page<Work> findAllByGenre(com.stolink.backend.domain.work.entity.Genre genre, Pageable pageable);

        /**
         * 진르별 작품 조회 (읽은 작품 제외)
         */
        @EntityGraph(attributePaths = { "author" })
        Page<Work> findByGenreAndIdNotIn(com.stolink.backend.domain.work.entity.Genre genre, List<UUID> ids,
                        Pageable pageable);

        /**
         * 여러 장르로 조회
         */
        @EntityGraph(attributePaths = { "author" })
        Page<Work> findByGenreIn(List<com.stolink.backend.domain.work.entity.Genre> genres, Pageable pageable);

        /**
         * 기간별 좋아요 순 랭킹 조회
         * - WorkLike 테이블을 기준으로 집계
         *
         * ⚠️ 성능 경고: 데이터가 수십만 건 이상 쌓일 경우 성능 저하 우려
         * 권장 개선사항:
         * - 랭킹 전용 집계 테이블 운영 (일간/주간 배치 집계)
         * - Redis를 활용한 실시간 랭킹 캐싱
         */
        @Query("SELECT wl.work FROM WorkLike wl " +
                        "WHERE wl.createdAt >= :startDate " +
                        "GROUP BY wl.work " +
                        "ORDER BY COUNT(wl) DESC, wl.work.likeCount DESC")
        Page<Work> findRankingByPeriod(@Param("startDate") java.time.LocalDateTime startDate, Pageable pageable);

        /**
         * 기간별 랭킹 조회 (장르, AccessType 필터 포함)
         */
        @Query("SELECT wl.work FROM WorkLike wl " +
                        "WHERE wl.createdAt >= :startDate " +
                        "AND (:genre IS NULL OR wl.work.genre = :genre) " +
                        "AND (:accessType IS NULL OR " +
                        "  (:accessType = 'FREE' AND NOT EXISTS (SELECT c FROM Chapter c WHERE c.work = wl.work AND c.isFree = false)) OR "
                        +
                        "  (:accessType = 'PAID' AND EXISTS (SELECT c FROM Chapter c WHERE c.work = wl.work AND c.isFree = false)) "
                        +
                        ") " +
                        "GROUP BY wl.work " +
                        "ORDER BY COUNT(wl) DESC, wl.work.likeCount DESC")
        Page<Work> findRankingByPeriodWithFilter(
                        @Param("startDate") java.time.LocalDateTime startDate,
                        @Param("genre") com.stolink.backend.domain.work.entity.Genre genre,
                        @Param("accessType") String accessType,
                        Pageable pageable);

        /**
         * 모든 작품의 좋아요 수를 실제 WorkLike 테이블 기준으로 일괄 동기화
         * N+1 문제 해결: 단일 벌크 업데이트 쿼리로 처리
         */
        @Modifying
        @Query("UPDATE Work w SET w.likeCount = (SELECT COUNT(wl) FROM WorkLike wl WHERE wl.work = w)")
        void syncAllLikeCounts();

        /**
         * 좋아요 수 원자적 증가 (Race Condition 방지)
         */
        @Modifying
        @Query("UPDATE Work w SET w.likeCount = w.likeCount + 1 WHERE w.id = :id")
        void incrementLikeCount(@Param("id") UUID id);

        /**
         * 좋아요 수 원자적 감소 (Race Condition 방지)
         */
        @Modifying
        @Query("UPDATE Work w SET w.likeCount = CASE WHEN w.likeCount > 0 THEN w.likeCount - 1 ELSE 0 END WHERE w.id = :id")
        void decrementLikeCount(@Param("id") UUID id);
}
