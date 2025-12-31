package com.stolink.backend.domain.work.repository;

import com.stolink.backend.domain.work.entity.Work;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkRepository extends JpaRepository<Work, UUID> {

    Page<Work> findByAuthorId(UUID authorId, Pageable pageable);

    Optional<Work> findByIdAndAuthorId(UUID id, UUID authorId);

    @Query("SELECT w FROM Work w WHERE w.title LIKE %:keyword% OR w.author.nickname LIKE %:keyword%")
    Page<Work> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT w FROM Work w ORDER BY w.createdAt DESC")
    Page<Work> findAllOrderByCreatedAtDesc(Pageable pageable);
}
