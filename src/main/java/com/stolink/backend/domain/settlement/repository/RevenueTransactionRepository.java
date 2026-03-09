package com.stolink.backend.domain.settlement.repository;

import com.stolink.backend.domain.settlement.entity.RevenueTransaction;
import com.stolink.backend.domain.settlement.entity.RevenueTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface RevenueTransactionRepository extends JpaRepository<RevenueTransaction, UUID> {

    Page<RevenueTransaction> findByAuthorIdOrderByCreatedAtDesc(UUID authorId, Pageable pageable);

    Page<RevenueTransaction> findByAuthorIdAndTypeOrderByCreatedAtDesc(
            UUID authorId, RevenueTransactionType type, Pageable pageable);

    @Query("SELECT rt FROM RevenueTransaction rt WHERE rt.authorId = :authorId " +
            "AND rt.settlementId IS NULL " +
            "AND rt.createdAt >= :start AND rt.createdAt < :end " +
            "ORDER BY rt.createdAt ASC")
    List<RevenueTransaction> findUnsettledByAuthorIdAndPeriod(
            @Param("authorId") UUID authorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    List<RevenueTransaction> findBySettlementIdOrderByCreatedAtAsc(UUID settlementId);

    boolean existsByPurchaseIdAndType(UUID purchaseId, RevenueTransactionType type);

    @Query("SELECT rt.workId, SUM(rt.authorShare), COUNT(rt) " +
            "FROM RevenueTransaction rt " +
            "WHERE rt.authorId = :authorId AND rt.type = 'CHAPTER_SALE' " +
            "GROUP BY rt.workId")
    List<Object[]> findRevenueByWork(@Param("authorId") UUID authorId);

    @Query(value = "SELECT DATE_TRUNC('month', rt.created_at) AS month, " +
            "SUM(rt.author_share) AS total_share, COUNT(*) AS tx_count " +
            "FROM revenue_transactions rt " +
            "WHERE rt.author_id = :authorId AND rt.type = 'CHAPTER_SALE' " +
            "GROUP BY DATE_TRUNC('month', rt.created_at) " +
            "ORDER BY month DESC",
            nativeQuery = true)
    List<Object[]> findMonthlyRevenue(@Param("authorId") UUID authorId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE RevenueTransaction rt SET rt.settlementId = :settlementId " +
            "WHERE rt.authorId = :authorId AND rt.settlementId IS NULL " +
            "AND rt.createdAt >= :start AND rt.createdAt < :end")
    int assignSettlement(
            @Param("settlementId") UUID settlementId,
            @Param("authorId") UUID authorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE RevenueTransaction rt SET rt.settlementId = NULL " +
            "WHERE rt.settlementId = :settlementId")
    int releaseSettlement(@Param("settlementId") UUID settlementId);
}
