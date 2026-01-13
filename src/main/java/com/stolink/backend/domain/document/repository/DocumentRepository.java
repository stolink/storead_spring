package com.stolink.backend.domain.document.repository;

import com.stolink.backend.domain.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Document d SET d.isPublished = :status WHERE d.id IN :ids")
    void updatePublishStatus(@Param("ids") List<UUID> ids, @Param("status") boolean status);
}
