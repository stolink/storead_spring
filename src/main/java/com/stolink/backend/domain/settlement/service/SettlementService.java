package com.stolink.backend.domain.settlement.service;

import com.stolink.backend.domain.settlement.config.SettlementConfig;
import com.stolink.backend.domain.settlement.dto.DashboardResponse;
import com.stolink.backend.domain.settlement.dto.RevenueTransactionResponse;
import com.stolink.backend.domain.settlement.dto.SettlementResponse;
import com.stolink.backend.domain.settlement.entity.AuthorRevenue;
import com.stolink.backend.domain.settlement.entity.RevenueTransaction;
import com.stolink.backend.domain.settlement.entity.Settlement;
import com.stolink.backend.domain.settlement.exception.SettlementExceptions;
import com.stolink.backend.domain.settlement.repository.AuthorRevenueRepository;
import com.stolink.backend.domain.settlement.repository.RevenueTransactionRepository;
import com.stolink.backend.domain.settlement.repository.SettlementRepository;
import com.stolink.backend.domain.work.entity.Work;
import com.stolink.backend.domain.work.repository.WorkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final RevenueTransactionRepository revenueTransactionRepository;
    private final AuthorRevenueRepository authorRevenueRepository;
    private final WorkRepository workRepository;
    private final SettlementConfig settlementConfig;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * 정산 생성 (작가가 정산 요청)
     */
    @Transactional
    public SettlementResponse createSettlement(UUID authorId, LocalDate periodStart, LocalDate periodEnd) {
        // 중복 정산 방지
        if (settlementRepository.existsByAuthorIdAndPeriodStartAndPeriodEnd(authorId, periodStart, periodEnd)) {
            throw new SettlementExceptions.DuplicateSettlementException(
                    String.format("이미 해당 기간의 정산이 존재합니다: %s ~ %s", periodStart, periodEnd));
        }

        // 미정산 거래 조회
        LocalDateTime startTime = periodStart.atStartOfDay();
        LocalDateTime endTime = periodEnd.atStartOfDay();
        List<RevenueTransaction> transactions = revenueTransactionRepository
                .findUnsettledByAuthorIdAndPeriod(authorId, startTime, endTime);

        if (transactions.isEmpty()) {
            throw new SettlementExceptions.InsufficientRevenueException("정산할 거래가 없습니다.");
        }

        // 집계
        long grossAmount = 0;
        long platformFeeTotal = 0;
        long netAmount = 0;
        for (RevenueTransaction tx : transactions) {
            grossAmount += tx.getCreditAmount();
            platformFeeTotal += tx.getPlatformFee();
            netAmount += tx.getAuthorShare();
        }

        // 최소 정산 금액 확인
        if (netAmount < settlementConfig.getMinimumSettlementAmount()) {
            throw new SettlementExceptions.InsufficientRevenueException(
                    String.format("최소 정산 금액(%d 크레딧)에 미달합니다. 현재: %d",
                            settlementConfig.getMinimumSettlementAmount(), netAmount));
        }

        // 정산 생성
        Settlement settlement = Settlement.builder()
                .authorId(authorId)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .grossAmount(grossAmount)
                .platformFeeTotal(platformFeeTotal)
                .netAmount(netAmount)
                .transactionCount(transactions.size())
                .build();

        settlement = settlementRepository.save(settlement);

        // 거래에 정산 ID 할당
        revenueTransactionRepository.assignSettlement(
                settlement.getId(), authorId, startTime, endTime);

        log.info("정산 생성: authorId={}, period={} ~ {}, netAmount={}, txCount={}",
                authorId, periodStart, periodEnd, netAmount, transactions.size());

        return SettlementResponse.from(settlement);
    }

    /**
     * 작가 정산 확인
     */
    @Transactional
    public void confirmSettlement(UUID authorId, UUID settlementId) {
        Settlement settlement = settlementRepository.findByIdAndAuthorId(settlementId, authorId)
                .orElseThrow(() -> new SettlementExceptions.SettlementNotFoundException(
                        "정산을 찾을 수 없습니다: " + settlementId));
        settlement.confirm();
    }

    /**
     * 작가 정산 거절
     */
    @Transactional
    public void rejectSettlement(UUID authorId, UUID settlementId, String reason) {
        Settlement settlement = settlementRepository.findByIdAndAuthorId(settlementId, authorId)
                .orElseThrow(() -> new SettlementExceptions.SettlementNotFoundException(
                        "정산을 찾을 수 없습니다: " + settlementId));
        settlement.reject(reason);
        revenueTransactionRepository.releaseSettlement(settlementId);

        log.info("정산 거절: settlementId={}, reason={}", settlementId, reason);
    }

    /**
     * 정산 처리 (관리자/스케줄러)
     */
    @Transactional
    public void processSettlement(UUID settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new SettlementExceptions.SettlementNotFoundException(
                        "정산을 찾을 수 없습니다: " + settlementId));

        settlement.startProcessing();

        try {
            AuthorRevenue revenue = authorRevenueRepository.findByAuthorIdWithLock(settlement.getAuthorId())
                    .orElseThrow(() -> new SettlementExceptions.AuthorRevenueNotFoundException(
                            "작가 수익 정보를 찾을 수 없습니다."));

            revenue.settle(settlement.getNetAmount());
            settlement.complete();

            log.info("정산 완료: settlementId={}, netAmount={}", settlementId, settlement.getNetAmount());
        } catch (Exception e) {
            settlement.fail(e.getMessage());
            log.error("정산 실패: settlementId={}, error={}", settlementId, e.getMessage());
        }
    }

    /**
     * 대시보드 조회
     */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(UUID authorId) {
        AuthorRevenue revenue = authorRevenueRepository.findByAuthorId(authorId).orElse(null);
        if (revenue == null) {
            return DashboardResponse.empty();
        }

        // 작품별 수익
        List<DashboardResponse.WorkRevenueItem> workItems = buildWorkRevenueItems(authorId);

        // 월별 수익
        List<DashboardResponse.MonthlyRevenueItem> monthlyItems = buildMonthlyRevenueItems(authorId);

        // 최근 거래
        Page<RevenueTransaction> recentPage = revenueTransactionRepository
                .findByAuthorIdOrderByCreatedAtDesc(authorId, PageRequest.of(0, 10));
        List<RevenueTransactionResponse> recentTransactions = recentPage.getContent()
                .stream()
                .map(RevenueTransactionResponse::from)
                .toList();

        return new DashboardResponse(
                revenue.getTotalEarned(),
                revenue.getPendingBalance(),
                revenue.getTotalSettled(),
                workItems,
                monthlyItems,
                recentTransactions
        );
    }

    /**
     * 정산 내역 페이징 조회
     */
    @Transactional(readOnly = true)
    public Page<SettlementResponse> getSettlements(UUID authorId, Pageable pageable) {
        return settlementRepository.findByAuthorIdOrderByPeriodStartDesc(authorId, pageable)
                .map(SettlementResponse::from);
    }

    /**
     * 수익 거래 내역 페이징 조회
     */
    @Transactional(readOnly = true)
    public Page<RevenueTransactionResponse> getRevenueTransactions(UUID authorId, Pageable pageable) {
        return revenueTransactionRepository.findByAuthorIdOrderByCreatedAtDesc(authorId, pageable)
                .map(RevenueTransactionResponse::from);
    }

    private List<DashboardResponse.WorkRevenueItem> buildWorkRevenueItems(UUID authorId) {
        List<Object[]> rawData = revenueTransactionRepository.findRevenueByWork(authorId);
        List<DashboardResponse.WorkRevenueItem> items = new ArrayList<>();

        for (Object[] row : rawData) {
            UUID workId = (UUID) row[0];
            Long totalRevenue = ((Number) row[1]).longValue();
            Long txCount = ((Number) row[2]).longValue();

            String workTitle = workRepository.findById(workId)
                    .map(Work::getTitle)
                    .orElse("삭제된 작품");

            items.add(new DashboardResponse.WorkRevenueItem(workId, workTitle, totalRevenue, txCount));
        }
        return items;
    }

    private List<DashboardResponse.MonthlyRevenueItem> buildMonthlyRevenueItems(UUID authorId) {
        List<Object[]> rawData = revenueTransactionRepository.findMonthlyRevenue(authorId);
        List<DashboardResponse.MonthlyRevenueItem> items = new ArrayList<>();

        for (Object[] row : rawData) {
            Timestamp ts = (Timestamp) row[0];
            String month = ts.toLocalDateTime().format(MONTH_FORMATTER);
            Long totalRevenue = ((Number) row[1]).longValue();
            Long txCount = ((Number) row[2]).longValue();

            items.add(new DashboardResponse.MonthlyRevenueItem(month, totalRevenue, txCount));
        }
        return items;
    }
}
