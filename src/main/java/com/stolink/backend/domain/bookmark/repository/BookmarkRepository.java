package com.stolink.backend.domain.bookmark.repository;

import com.stolink.backend.domain.bookmark.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookmarkRepository extends JpaRepository<Bookmark, UUID> {

    Optional<Bookmark> findByUserIdAndChapterId(UUID userId, UUID chapterId);

    @Query("SELECT b FROM Bookmark b WHERE b.user.id = :userId AND b.chapter.work.id = :workId ORDER BY b.updatedAt DESC")
    List<Bookmark> findByUserIdAndWorkIdOrderByUpdatedAtDesc(
            @Param("userId") UUID userId,
            @Param("workId") UUID workId);

    @Query("SELECT COUNT(DISTINCT b.chapter.id) FROM Bookmark b WHERE b.user.id = :userId AND b.chapter.work.id = :workId")
    int countReadChaptersByUserIdAndWorkId(
            @Param("userId") UUID userId,
            @Param("workId") UUID workId);
}
