package com.stolink.backend.domain.work.repository;

import com.stolink.backend.domain.work.entity.Work;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkRepository extends JpaRepository<Work, UUID> {

    @Override
    @EntityGraph(attributePaths = {"author"})
    Page<Work> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    Page<Work> findByAuthorId(UUID authorId, Pageable pageable);

    Optional<Work> findByIdAndAuthorId(UUID id, UUID authorId);

    /**
     * 키워드로 작품 검색 (N+1 문제 해결 - @EntityGraph 사용)
     */
    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT w FROM Work w WHERE w.title LIKE %:keyword% OR w.author.nickname LIKE %:keyword%")
    Page<Work> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 작품 ID 목록으로 챕터 수 조회 (N+1 문제 해결 - 배치 조회)
     */
    @Query("SELECT c.work.id, COUNT(c) FROM Chapter c WHERE c.work.id IN :workIds GROUP BY c.work.id")
    List<Object[]> countChaptersByWorkIds(@Param("workIds") List<UUID> workIds);

    @EntityGraph(attributePaths = {"author"})
    @Query("SELECT w FROM Work w ORDER BY w.createdAt DESC")
    Page<Work> findAllOrderByCreatedAtDesc(Pageable pageable);
}

