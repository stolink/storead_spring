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

    void deleteByWorkId(UUID workId);
}
