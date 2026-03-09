package com.stolink.backend.domain.settlement.repository;

import com.stolink.backend.domain.settlement.entity.AuthorRevenue;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AuthorRevenueRepository extends JpaRepository<AuthorRevenue, UUID> {

    Optional<AuthorRevenue> findByAuthorId(UUID authorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ar FROM AuthorRevenue ar WHERE ar.authorId = :authorId")
    Optional<AuthorRevenue> findByAuthorIdWithLock(@Param("authorId") UUID authorId);
}
