package com.stolink.backend.domain.settlement.service;

import com.stolink.backend.domain.chapter.entity.Chapter;
import com.stolink.backend.domain.chapter.entity.ChapterPurchase;
import com.stolink.backend.domain.chapter.repository.ChapterRepository;
import com.stolink.backend.domain.settlement.config.SettlementConfig;
import com.stolink.backend.domain.settlement.entity.AuthorRevenue;
import com.stolink.backend.domain.settlement.entity.RevenueTransaction;
import com.stolink.backend.domain.settlement.entity.RevenueTransactionType;
import com.stolink.backend.domain.settlement.exception.SettlementExceptions;
import com.stolink.backend.domain.settlement.repository.AuthorRevenueRepository;
import com.stolink.backend.domain.settlement.repository.RevenueTransactionRepository;
import com.stolink.backend.domain.work.entity.Genre;
import com.stolink.backend.domain.work.entity.Work;
import com.stolink.backend.domain.work.entity.WorkStatus;
import com.stolink.backend.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevenueServiceTest {

    @InjectMocks
    private RevenueService revenueService;

    @Mock
    private AuthorRevenueRepository authorRevenueRepository;

    @Mock
    private RevenueTransactionRepository revenueTransactionRepository;

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private SettlementConfig settlementConfig;

    private UUID authorId;
    private UUID buyerId;
    private UUID workId;
    private UUID chapterId;
    private UUID purchaseId;

    @BeforeEach
    void setUp() {
        authorId = UUID.randomUUID();
        buyerId = UUID.randomUUID();
        workId = UUID.randomUUID();
        chapterId = UUID.randomUUID();
        purchaseId = UUID.randomUUID();
    }

    private Chapter createChapter(int price) {
        User author = User.builder().build();
        ReflectionTestUtils.setField(author, "id", authorId);

        Work work = Work.builder()
                .author(author)
                .title("Test Work")
                .synopsis("Test")
                .genre(Genre.FANTASY)
                .status(WorkStatus.ONGOING)
                .build();
        ReflectionTestUtils.setField(work, "id", workId);

        Chapter chapter = Chapter.builder()
                .work(work)
                .title("Chapter 1")
                .content("Content")
                .chapterNumber(1)
                .isFree(false)
                .price(price)
                .build();
        ReflectionTestUtils.setField(chapter, "id", chapterId);
        return chapter;
    }

    private ChapterPurchase createPurchase(int price) {
        ChapterPurchase purchase = ChapterPurchase.builder()
                .userId(buyerId)
                .chapterId(chapterId)
                .pricePaid(price)
                .build();
        ReflectionTestUtils.setField(purchase, "id", purchaseId);
        return purchase;
    }

    @Nested
    @DisplayName("recordChapterSaleRevenue")
    class RecordChapterSaleRevenue {

        @Test
        @DisplayName("챕터 판매 시 수익이 정상 기록된다")
        void shouldRecordRevenueOnChapterSale() {
            // given
            int price = 100;
            Chapter chapter = createChapter(price);
            ChapterPurchase purchase = createPurchase(price);

            given(settlementConfig.getPlatformFeeRate()).willReturn(0.3);
            given(chapterRepository.findById(chapterId)).willReturn(Optional.of(chapter));
            given(revenueTransactionRepository.existsByPurchaseIdAndType(purchaseId, RevenueTransactionType.CHAPTER_SALE))
                    .willReturn(false);

            AuthorRevenue revenue = AuthorRevenue.createForAuthor(authorId);
            ReflectionTestUtils.setField(revenue, "id", UUID.randomUUID());
            given(authorRevenueRepository.findByAuthorIdWithLock(authorId)).willReturn(Optional.of(revenue));

            // when
            revenueService.recordChapterSaleRevenue(purchase);

            // then
            ArgumentCaptor<RevenueTransaction> txCaptor = ArgumentCaptor.forClass(RevenueTransaction.class);
            verify(revenueTransactionRepository).save(txCaptor.capture());

            RevenueTransaction savedTx = txCaptor.getValue();
            assertThat(savedTx.getAuthorId()).isEqualTo(authorId);
            assertThat(savedTx.getCreditAmount()).isEqualTo(100);
            assertThat(savedTx.getPlatformFee()).isEqualTo(30); // 100 * 0.3
            assertThat(savedTx.getAuthorShare()).isEqualTo(70); // 100 - 30
            assertThat(savedTx.getType()).isEqualTo(RevenueTransactionType.CHAPTER_SALE);

            // 수익 잔액 확인
            assertThat(revenue.getTotalEarned()).isEqualTo(70L);
            assertThat(revenue.getPendingBalance()).isEqualTo(70L);
        }

        @Test
        @DisplayName("작가 본인이 구매한 경우 수익 미생성")
        void shouldNotRecordRevenueWhenAuthorPurchasesOwnChapter() {
            // given
            int price = 100;
            Chapter chapter = createChapter(price);
            // buyer == author
            ChapterPurchase purchase = ChapterPurchase.builder()
                    .userId(authorId)
                    .chapterId(chapterId)
                    .pricePaid(price)
                    .build();
            ReflectionTestUtils.setField(purchase, "id", purchaseId);

            given(chapterRepository.findById(chapterId)).willReturn(Optional.of(chapter));

            // when
            revenueService.recordChapterSaleRevenue(purchase);

            // then
            verify(revenueTransactionRepository, never()).save(any());
            verify(authorRevenueRepository, never()).findByAuthorIdWithLock(any());
        }

        @Test
        @DisplayName("중복 구매에 대한 수익 생성 방지 (멱등성)")
        void shouldPreventDuplicateRevenue() {
            // given
            int price = 100;
            Chapter chapter = createChapter(price);
            ChapterPurchase purchase = createPurchase(price);

            given(chapterRepository.findById(chapterId)).willReturn(Optional.of(chapter));
            given(revenueTransactionRepository.existsByPurchaseIdAndType(purchaseId, RevenueTransactionType.CHAPTER_SALE))
                    .willReturn(true);

            // when
            revenueService.recordChapterSaleRevenue(purchase);

            // then
            verify(revenueTransactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("AuthorRevenue가 없으면 새로 생성하여 수익 기록")
        void shouldCreateAuthorRevenueIfNotExists() {
            // given
            int price = 200;
            Chapter chapter = createChapter(price);
            ChapterPurchase purchase = createPurchase(price);

            given(settlementConfig.getPlatformFeeRate()).willReturn(0.3);
            given(chapterRepository.findById(chapterId)).willReturn(Optional.of(chapter));
            given(revenueTransactionRepository.existsByPurchaseIdAndType(purchaseId, RevenueTransactionType.CHAPTER_SALE))
                    .willReturn(false);
            given(authorRevenueRepository.findByAuthorIdWithLock(authorId)).willReturn(Optional.empty());

            AuthorRevenue newRevenue = AuthorRevenue.createForAuthor(authorId);
            ReflectionTestUtils.setField(newRevenue, "id", UUID.randomUUID());
            given(authorRevenueRepository.save(any(AuthorRevenue.class))).willReturn(newRevenue);

            // when
            revenueService.recordChapterSaleRevenue(purchase);

            // then
            verify(authorRevenueRepository).save(any(AuthorRevenue.class));
            verify(revenueTransactionRepository).save(any(RevenueTransaction.class));
            assertThat(newRevenue.getTotalEarned()).isEqualTo(140L); // 200 - 60(fee)
        }

        @Test
        @DisplayName("DataIntegrityViolation 시 멱등하게 무시")
        void shouldHandleConcurrentDuplicateGracefully() {
            // given
            int price = 100;
            Chapter chapter = createChapter(price);
            ChapterPurchase purchase = createPurchase(price);

            given(settlementConfig.getPlatformFeeRate()).willReturn(0.3);
            given(chapterRepository.findById(chapterId)).willReturn(Optional.of(chapter));
            given(revenueTransactionRepository.existsByPurchaseIdAndType(purchaseId, RevenueTransactionType.CHAPTER_SALE))
                    .willReturn(false);

            AuthorRevenue revenue = AuthorRevenue.createForAuthor(authorId);
            ReflectionTestUtils.setField(revenue, "id", UUID.randomUUID());
            given(authorRevenueRepository.findByAuthorIdWithLock(authorId)).willReturn(Optional.of(revenue));
            given(revenueTransactionRepository.save(any(RevenueTransaction.class)))
                    .willThrow(new DataIntegrityViolationException("Duplicate key"));

            // when - 예외 없이 무시
            revenueService.recordChapterSaleRevenue(purchase);

            // then
            verify(revenueTransactionRepository).save(any());
        }

        @Test
        @DisplayName("무료 챕터(가격 0) 구매 시 수익 미생성")
        void shouldNotRecordRevenueForFreeChapter() {
            // given
            Chapter chapter = createChapter(0);
            ReflectionTestUtils.setField(chapter, "isFree", true);
            ChapterPurchase purchase = createPurchase(0);

            given(chapterRepository.findById(chapterId)).willReturn(Optional.of(chapter));

            // when
            revenueService.recordChapterSaleRevenue(purchase);

            // then
            verify(revenueTransactionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("recordRefundRevenue")
    class RecordRefundRevenue {

        @Test
        @DisplayName("환불 시 수익이 차감된다")
        void shouldDeductRevenueOnRefund() {
            // given
            int price = 100;
            Chapter chapter = createChapter(price);
            ChapterPurchase purchase = createPurchase(price);

            given(settlementConfig.getPlatformFeeRate()).willReturn(0.3);
            given(chapterRepository.findById(chapterId)).willReturn(Optional.of(chapter));
            given(revenueTransactionRepository.existsByPurchaseIdAndType(purchaseId, RevenueTransactionType.REFUND))
                    .willReturn(false);

            AuthorRevenue revenue = AuthorRevenue.createForAuthor(authorId);
            ReflectionTestUtils.setField(revenue, "id", UUID.randomUUID());
            revenue.addEarning(70L); // 기존 수익
            given(authorRevenueRepository.findByAuthorIdWithLock(authorId)).willReturn(Optional.of(revenue));

            // when
            revenueService.recordRefundRevenue(purchase);

            // then
            ArgumentCaptor<RevenueTransaction> txCaptor = ArgumentCaptor.forClass(RevenueTransaction.class);
            verify(revenueTransactionRepository).save(txCaptor.capture());

            RevenueTransaction savedTx = txCaptor.getValue();
            assertThat(savedTx.getType()).isEqualTo(RevenueTransactionType.REFUND);
            assertThat(savedTx.getAuthorShare()).isEqualTo(-70);

            assertThat(revenue.getTotalEarned()).isEqualTo(0L);
            assertThat(revenue.getPendingBalance()).isEqualTo(0L);
        }

        @Test
        @DisplayName("수익 잔액 부족 시 예외 발생")
        void shouldThrowWhenInsufficientBalance() {
            // given
            int price = 100;
            Chapter chapter = createChapter(price);
            ChapterPurchase purchase = createPurchase(price);

            given(settlementConfig.getPlatformFeeRate()).willReturn(0.3);
            given(chapterRepository.findById(chapterId)).willReturn(Optional.of(chapter));
            given(revenueTransactionRepository.existsByPurchaseIdAndType(purchaseId, RevenueTransactionType.REFUND))
                    .willReturn(false);

            AuthorRevenue revenue = AuthorRevenue.createForAuthor(authorId);
            ReflectionTestUtils.setField(revenue, "id", UUID.randomUUID());
            // 수익 없음 - 차감 불가
            given(authorRevenueRepository.findByAuthorIdWithLock(authorId)).willReturn(Optional.of(revenue));

            // when & then
            assertThatThrownBy(() -> revenueService.recordRefundRevenue(purchase))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("부족합니다");
        }
    }
}
