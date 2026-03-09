package com.stolink.backend.domain.settlement.service;

import com.stolink.backend.domain.settlement.config.SettlementConfig;
import com.stolink.backend.domain.settlement.dto.DashboardResponse;
import com.stolink.backend.domain.settlement.dto.SettlementResponse;
import com.stolink.backend.domain.settlement.entity.*;
import com.stolink.backend.domain.settlement.exception.SettlementExceptions;
import com.stolink.backend.domain.settlement.repository.AuthorRevenueRepository;
import com.stolink.backend.domain.settlement.repository.RevenueTransactionRepository;
import com.stolink.backend.domain.settlement.repository.SettlementRepository;
import com.stolink.backend.domain.work.entity.Work;
import com.stolink.backend.domain.work.repository.WorkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @InjectMocks
    private SettlementService settlementService;

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private RevenueTransactionRepository revenueTransactionRepository;

    @Mock
    private AuthorRevenueRepository authorRevenueRepository;

    @Mock
    private WorkRepository workRepository;

    @Mock
    private SettlementConfig settlementConfig;

    private UUID authorId;

    @BeforeEach
    void setUp() {
        authorId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("createSettlement - 정산 생성")
    class CreateSettlement {

        @Test
        @DisplayName("미정산 거래가 있으면 정산 생성 성공")
        void shouldCreateSettlementWithUnsettledTransactions() {
            // given
            LocalDate periodStart = LocalDate.of(2026, 2, 1);
            LocalDate periodEnd = LocalDate.of(2026, 3, 1);

            given(settlementRepository.existsByAuthorIdAndPeriodStartAndPeriodEnd(authorId, periodStart, periodEnd))
                    .willReturn(false);
            given(settlementConfig.getMinimumSettlementAmount()).willReturn(100L);

            RevenueTransaction tx1 = RevenueTransaction.builder()
                    .authorId(authorId)
                    .workId(UUID.randomUUID())
                    .chapterId(UUID.randomUUID())
                    .buyerUserId(UUID.randomUUID())
                    .purchaseId(UUID.randomUUID())
                    .type(RevenueTransactionType.CHAPTER_SALE)
                    .creditAmount(100L)
                    .platformFeeRate(0.3)
                    .platformFee(30L)
                    .authorShare(70L)
                    .build();
            RevenueTransaction tx2 = RevenueTransaction.builder()
                    .authorId(authorId)
                    .workId(UUID.randomUUID())
                    .chapterId(UUID.randomUUID())
                    .buyerUserId(UUID.randomUUID())
                    .purchaseId(UUID.randomUUID())
                    .type(RevenueTransactionType.CHAPTER_SALE)
                    .creditAmount(200L)
                    .platformFeeRate(0.3)
                    .platformFee(60L)
                    .authorShare(140L)
                    .build();

            given(revenueTransactionRepository.findUnsettledByAuthorIdAndPeriod(
                    eq(authorId),
                    eq(periodStart.atStartOfDay()),
                    eq(periodEnd.atStartOfDay())))
                    .willReturn(List.of(tx1, tx2));

            Settlement savedSettlement = Settlement.builder()
                    .authorId(authorId)
                    .periodStart(periodStart)
                    .periodEnd(periodEnd)
                    .grossAmount(300L)
                    .platformFeeTotal(90L)
                    .netAmount(210L)
                    .transactionCount(2)
                    .build();
            ReflectionTestUtils.setField(savedSettlement, "id", UUID.randomUUID());

            given(settlementRepository.save(any(Settlement.class))).willReturn(savedSettlement);

            // when
            SettlementResponse response = settlementService.createSettlement(authorId, periodStart, periodEnd);

            // then
            assertThat(response.grossAmount()).isEqualTo(300L);
            assertThat(response.platformFeeTotal()).isEqualTo(90L);
            assertThat(response.netAmount()).isEqualTo(210L);
            assertThat(response.transactionCount()).isEqualTo(2);

            verify(revenueTransactionRepository).assignSettlement(
                    any(UUID.class), eq(authorId),
                    eq(periodStart.atStartOfDay()),
                    eq(periodEnd.atStartOfDay()));
        }

        @Test
        @DisplayName("동일 기간 중복 정산 요청 시 예외")
        void shouldThrowOnDuplicateSettlement() {
            // given
            LocalDate periodStart = LocalDate.of(2026, 2, 1);
            LocalDate periodEnd = LocalDate.of(2026, 3, 1);

            given(settlementRepository.existsByAuthorIdAndPeriodStartAndPeriodEnd(authorId, periodStart, periodEnd))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> settlementService.createSettlement(authorId, periodStart, periodEnd))
                    .isInstanceOf(SettlementExceptions.DuplicateSettlementException.class);
        }

        @Test
        @DisplayName("미정산 거래가 없으면 예외")
        void shouldThrowWhenNoUnsettledTransactions() {
            // given
            LocalDate periodStart = LocalDate.of(2026, 2, 1);
            LocalDate periodEnd = LocalDate.of(2026, 3, 1);

            given(settlementRepository.existsByAuthorIdAndPeriodStartAndPeriodEnd(authorId, periodStart, periodEnd))
                    .willReturn(false);
            given(revenueTransactionRepository.findUnsettledByAuthorIdAndPeriod(
                    eq(authorId), any(), any())).willReturn(List.of());

            // when & then
            assertThatThrownBy(() -> settlementService.createSettlement(authorId, periodStart, periodEnd))
                    .isInstanceOf(SettlementExceptions.InsufficientRevenueException.class);
        }

        @Test
        @DisplayName("최소 정산 금액 미달 시 예외")
        void shouldThrowWhenBelowMinimumAmount() {
            // given
            LocalDate periodStart = LocalDate.of(2026, 2, 1);
            LocalDate periodEnd = LocalDate.of(2026, 3, 1);

            given(settlementRepository.existsByAuthorIdAndPeriodStartAndPeriodEnd(authorId, periodStart, periodEnd))
                    .willReturn(false);
            given(settlementConfig.getMinimumSettlementAmount()).willReturn(100L);

            RevenueTransaction tx = RevenueTransaction.builder()
                    .authorId(authorId)
                    .workId(UUID.randomUUID())
                    .chapterId(UUID.randomUUID())
                    .buyerUserId(UUID.randomUUID())
                    .purchaseId(UUID.randomUUID())
                    .type(RevenueTransactionType.CHAPTER_SALE)
                    .creditAmount(50L)
                    .platformFeeRate(0.3)
                    .platformFee(15L)
                    .authorShare(35L)
                    .build();

            given(revenueTransactionRepository.findUnsettledByAuthorIdAndPeriod(
                    eq(authorId), any(), any())).willReturn(List.of(tx));

            // when & then
            assertThatThrownBy(() -> settlementService.createSettlement(authorId, periodStart, periodEnd))
                    .isInstanceOf(SettlementExceptions.InsufficientRevenueException.class)
                    .hasMessageContaining("최소 정산 금액");
        }
    }

    @Nested
    @DisplayName("confirmSettlement - 작가 정산 확인")
    class ConfirmSettlement {

        @Test
        @DisplayName("PENDING 상태 정산을 확인할 수 있다")
        void shouldConfirmPendingSettlement() {
            // given
            UUID settlementId = UUID.randomUUID();
            Settlement settlement = Settlement.builder()
                    .authorId(authorId)
                    .periodStart(LocalDate.of(2026, 2, 1))
                    .periodEnd(LocalDate.of(2026, 3, 1))
                    .grossAmount(300L)
                    .platformFeeTotal(90L)
                    .netAmount(210L)
                    .transactionCount(2)
                    .build();
            ReflectionTestUtils.setField(settlement, "id", settlementId);

            given(settlementRepository.findByIdAndAuthorId(settlementId, authorId))
                    .willReturn(Optional.of(settlement));

            // when
            settlementService.confirmSettlement(authorId, settlementId);

            // then
            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.CONFIRMED);
            assertThat(settlement.getConfirmedAt()).isNotNull();
        }

        @Test
        @DisplayName("다른 작가의 정산 확인 시 예외")
        void shouldThrowWhenConfirmingOtherAuthorsSettlement() {
            // given
            UUID settlementId = UUID.randomUUID();
            UUID otherAuthorId = UUID.randomUUID();

            given(settlementRepository.findByIdAndAuthorId(settlementId, otherAuthorId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> settlementService.confirmSettlement(otherAuthorId, settlementId))
                    .isInstanceOf(SettlementExceptions.SettlementNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("rejectSettlement - 작가 정산 거절")
    class RejectSettlement {

        @Test
        @DisplayName("PENDING 상태 정산을 거절하면 거래가 해제된다")
        void shouldRejectAndReleaseTransactions() {
            // given
            UUID settlementId = UUID.randomUUID();
            Settlement settlement = Settlement.builder()
                    .authorId(authorId)
                    .periodStart(LocalDate.of(2026, 2, 1))
                    .periodEnd(LocalDate.of(2026, 3, 1))
                    .grossAmount(300L)
                    .platformFeeTotal(90L)
                    .netAmount(210L)
                    .transactionCount(2)
                    .build();
            ReflectionTestUtils.setField(settlement, "id", settlementId);

            given(settlementRepository.findByIdAndAuthorId(settlementId, authorId))
                    .willReturn(Optional.of(settlement));

            // when
            settlementService.rejectSettlement(authorId, settlementId, "금액이 맞지 않습니다");

            // then
            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.REJECTED);
            assertThat(settlement.getRejectReason()).isEqualTo("금액이 맞지 않습니다");
            verify(revenueTransactionRepository).releaseSettlement(settlementId);
        }
    }

    @Nested
    @DisplayName("processSettlement - 정산 처리")
    class ProcessSettlement {

        @Test
        @DisplayName("CONFIRMED 정산을 처리 완료하면 AuthorRevenue가 갱신된다")
        void shouldCompleteSettlementAndUpdateRevenue() {
            // given
            UUID settlementId = UUID.randomUUID();
            Settlement settlement = Settlement.builder()
                    .authorId(authorId)
                    .periodStart(LocalDate.of(2026, 2, 1))
                    .periodEnd(LocalDate.of(2026, 3, 1))
                    .grossAmount(300L)
                    .platformFeeTotal(90L)
                    .netAmount(210L)
                    .transactionCount(2)
                    .build();
            ReflectionTestUtils.setField(settlement, "id", settlementId);
            settlement.confirm();

            AuthorRevenue revenue = AuthorRevenue.createForAuthor(authorId);
            ReflectionTestUtils.setField(revenue, "id", UUID.randomUUID());
            revenue.addEarning(210L);

            given(settlementRepository.findById(settlementId)).willReturn(Optional.of(settlement));
            given(authorRevenueRepository.findByAuthorIdWithLock(authorId)).willReturn(Optional.of(revenue));

            // when
            settlementService.processSettlement(settlementId);

            // then
            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
            assertThat(revenue.getTotalSettled()).isEqualTo(210L);
            assertThat(revenue.getPendingBalance()).isEqualTo(0L);
        }

        @Test
        @DisplayName("정산 처리 중 AuthorRevenue 잔액 부족 시 FAILED 처리")
        void shouldFailSettlementWhenInsufficientBalance() {
            // given
            UUID settlementId = UUID.randomUUID();
            Settlement settlement = Settlement.builder()
                    .authorId(authorId)
                    .periodStart(LocalDate.of(2026, 2, 1))
                    .periodEnd(LocalDate.of(2026, 3, 1))
                    .grossAmount(300L)
                    .platformFeeTotal(90L)
                    .netAmount(210L)
                    .transactionCount(2)
                    .build();
            ReflectionTestUtils.setField(settlement, "id", settlementId);
            settlement.confirm();

            AuthorRevenue revenue = AuthorRevenue.createForAuthor(authorId);
            ReflectionTestUtils.setField(revenue, "id", UUID.randomUUID());
            // 잔액 부족 (환불이 발생했을 수 있음)

            given(settlementRepository.findById(settlementId)).willReturn(Optional.of(settlement));
            given(authorRevenueRepository.findByAuthorIdWithLock(authorId)).willReturn(Optional.of(revenue));

            // when
            settlementService.processSettlement(settlementId);

            // then
            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("getDashboard - 대시보드 조회")
    class GetDashboard {

        @Test
        @DisplayName("수익이 없는 작가는 빈 대시보드 반환")
        void shouldReturnEmptyDashboardForNewAuthor() {
            // given
            given(authorRevenueRepository.findByAuthorId(authorId)).willReturn(Optional.empty());

            // when
            DashboardResponse response = settlementService.getDashboard(authorId);

            // then
            assertThat(response.totalEarned()).isEqualTo(0L);
            assertThat(response.pendingBalance()).isEqualTo(0L);
            assertThat(response.totalSettled()).isEqualTo(0L);
            assertThat(response.revenueByWork()).isEmpty();
        }

        @Test
        @DisplayName("수익이 있는 작가는 전체 대시보드 반환")
        void shouldReturnFullDashboard() {
            // given
            AuthorRevenue revenue = AuthorRevenue.createForAuthor(authorId);
            ReflectionTestUtils.setField(revenue, "id", UUID.randomUUID());
            revenue.addEarning(500L);
            revenue.settle(200L);

            given(authorRevenueRepository.findByAuthorId(authorId)).willReturn(Optional.of(revenue));

            UUID workId1 = UUID.randomUUID();
            List<Object[]> workRevenueData = new ArrayList<>();
            workRevenueData.add(new Object[]{workId1, 500L, 5L});
            given(revenueTransactionRepository.findRevenueByWork(authorId))
                    .willReturn(workRevenueData);

            Work work1 = Work.builder().title("My Novel").synopsis("s").genre(com.stolink.backend.domain.work.entity.Genre.FANTASY)
                    .status(com.stolink.backend.domain.work.entity.WorkStatus.ONGOING)
                    .author(com.stolink.backend.domain.user.entity.User.builder().build())
                    .build();
            ReflectionTestUtils.setField(work1, "id", workId1);
            given(workRepository.findById(workId1)).willReturn(Optional.of(work1));

            List<Object[]> monthlyRevenueData = new ArrayList<>();
            monthlyRevenueData.add(new Object[]{
                    java.sql.Timestamp.valueOf(LocalDateTime.of(2026, 2, 1, 0, 0)), 500L, 5L});
            given(revenueTransactionRepository.findMonthlyRevenue(authorId))
                    .willReturn(monthlyRevenueData);

            given(revenueTransactionRepository.findByAuthorIdOrderByCreatedAtDesc(eq(authorId), any(Pageable.class)))
                    .willReturn(Page.empty());

            // when
            DashboardResponse response = settlementService.getDashboard(authorId);

            // then
            assertThat(response.totalEarned()).isEqualTo(500L);
            assertThat(response.pendingBalance()).isEqualTo(300L);
            assertThat(response.totalSettled()).isEqualTo(200L);
            assertThat(response.revenueByWork()).hasSize(1);
            assertThat(response.revenueByWork().get(0).workTitle()).isEqualTo("My Novel");
            assertThat(response.revenueByMonth()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getSettlements - 정산 내역 조회")
    class GetSettlements {

        @Test
        @DisplayName("작가의 정산 내역을 페이징 조회")
        void shouldReturnPagedSettlements() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Settlement s = Settlement.builder()
                    .authorId(authorId)
                    .periodStart(LocalDate.of(2026, 2, 1))
                    .periodEnd(LocalDate.of(2026, 3, 1))
                    .grossAmount(300L)
                    .platformFeeTotal(90L)
                    .netAmount(210L)
                    .transactionCount(2)
                    .build();
            ReflectionTestUtils.setField(s, "id", UUID.randomUUID());

            given(settlementRepository.findByAuthorIdOrderByPeriodStartDesc(authorId, pageable))
                    .willReturn(new PageImpl<>(List.of(s)));

            // when
            Page<SettlementResponse> result = settlementService.getSettlements(authorId, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).netAmount()).isEqualTo(210L);
        }
    }
}
