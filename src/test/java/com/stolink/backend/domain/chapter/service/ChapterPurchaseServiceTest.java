package com.stolink.backend.domain.chapter.service;

import com.stolink.backend.domain.chapter.dto.PurchaseCheckResponse;
import com.stolink.backend.domain.chapter.entity.Chapter;
import com.stolink.backend.domain.chapter.entity.ChapterAccessType;
import com.stolink.backend.domain.chapter.entity.ChapterPurchase;
import com.stolink.backend.domain.chapter.repository.ChapterPurchaseRepository;
import com.stolink.backend.domain.chapter.repository.ChapterRepository;
import com.stolink.backend.domain.payment.dto.request.CreditUseRequest;
import com.stolink.backend.domain.payment.dto.response.CreditCheckResponse;
import com.stolink.backend.domain.payment.dto.response.CreditResponse;
import com.stolink.backend.domain.payment.service.CreditService;
import com.stolink.backend.domain.settlement.service.RevenueService;
import com.stolink.backend.global.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class ChapterPurchaseServiceTest {

    @InjectMocks
    private ChapterPurchaseService chapterPurchaseService;

    @Mock
    private ChapterPurchaseRepository chapterPurchaseRepository;

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private CreditService creditService;

    @Mock
    private RevenueService revenueService;

    private Chapter createFreeChapter() {
        Chapter chapter = Chapter.builder()
                .title("무료 챕터")
                .content("내용")
                .chapterNumber(1)
                .isFree(true)
                .price(0)
                .accessType(ChapterAccessType.FREE)
                .build();
        ReflectionTestUtils.setField(chapter, "id", UUID.randomUUID());
        return chapter;
    }

    private Chapter createPaidChapter(int price) {
        Chapter chapter = Chapter.builder()
                .title("유료 챕터")
                .content("내용")
                .chapterNumber(2)
                .isFree(false)
                .price(price)
                .accessType(ChapterAccessType.PAID)
                .build();
        ReflectionTestUtils.setField(chapter, "id", UUID.randomUUID());
        return chapter;
    }

    @Nested
    @DisplayName("checkPurchaseAvailability")
    class CheckPurchaseAvailability {

        @Test
        @DisplayName("무료 챕터는 구매 가능 응답 (가격 0)")
        void freeChapter_ShouldReturnCanPurchase() {
            UUID userId = UUID.randomUUID();
            Chapter chapter = createFreeChapter();

            given(chapterRepository.findById(chapter.getId())).willReturn(Optional.of(chapter));

            PurchaseCheckResponse response = chapterPurchaseService.checkPurchaseAvailability(userId, chapter.getId());

            assertThat(response.isCanPurchase()).isTrue();
            assertThat(response.getChapterPrice()).isEqualTo(0);
            assertThat(response.isAlreadyPurchased()).isFalse();
        }

        @Test
        @DisplayName("이미 구매한 유료 챕터는 alreadyPurchased=true")
        void alreadyPurchased_ShouldReturnAlreadyPurchased() {
            UUID userId = UUID.randomUUID();
            Chapter chapter = createPaidChapter(100);

            given(chapterRepository.findById(chapter.getId())).willReturn(Optional.of(chapter));
            given(chapterPurchaseRepository.existsByUserIdAndChapterId(userId, chapter.getId())).willReturn(true);
            given(creditService.getCredit(userId))
                    .willReturn(new CreditResponse(UUID.randomUUID().toString(), 500L, 500L, 0L, null));

            PurchaseCheckResponse response = chapterPurchaseService.checkPurchaseAvailability(userId, chapter.getId());

            assertThat(response.isCanPurchase()).isTrue();
            assertThat(response.isAlreadyPurchased()).isTrue();
            assertThat(response.getCurrentBalance()).isEqualTo(500L);
        }

        @Test
        @DisplayName("잔액 충분한 유료 챕터는 구매 가능")
        void sufficientBalance_ShouldReturnCanPurchase() {
            UUID userId = UUID.randomUUID();
            Chapter chapter = createPaidChapter(100);

            given(chapterRepository.findById(chapter.getId())).willReturn(Optional.of(chapter));
            given(chapterPurchaseRepository.existsByUserIdAndChapterId(userId, chapter.getId())).willReturn(false);
            given(creditService.checkCredit(userId, 100L))
                    .willReturn(CreditCheckResponse.available(500L, 100L));

            PurchaseCheckResponse response = chapterPurchaseService.checkPurchaseAvailability(userId, chapter.getId());

            assertThat(response.isCanPurchase()).isTrue();
            assertThat(response.isAlreadyPurchased()).isFalse();
            assertThat(response.getCurrentBalance()).isEqualTo(500L);
            assertThat(response.getChapterPrice()).isEqualTo(100);
        }

        @Test
        @DisplayName("잔액 부족한 유료 챕터는 구매 불가")
        void insufficientBalance_ShouldReturnCannotPurchase() {
            UUID userId = UUID.randomUUID();
            Chapter chapter = createPaidChapter(100);

            given(chapterRepository.findById(chapter.getId())).willReturn(Optional.of(chapter));
            given(chapterPurchaseRepository.existsByUserIdAndChapterId(userId, chapter.getId())).willReturn(false);
            given(creditService.checkCredit(userId, 100L))
                    .willReturn(CreditCheckResponse.insufficient(50L, 100L));

            PurchaseCheckResponse response = chapterPurchaseService.checkPurchaseAvailability(userId, chapter.getId());

            assertThat(response.isCanPurchase()).isFalse();
            assertThat(response.getCurrentBalance()).isEqualTo(50L);
        }

        @Test
        @DisplayName("존재하지 않는 챕터 조회 시 예외 발생")
        void chapterNotFound_ShouldThrow() {
            UUID userId = UUID.randomUUID();
            UUID chapterId = UUID.randomUUID();

            given(chapterRepository.findById(chapterId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> chapterPurchaseService.checkPurchaseAvailability(userId, chapterId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("purchaseChapter")
    class PurchaseChapter {

        @Test
        @DisplayName("무료 챕터는 구매 처리 없이 리턴")
        void freeChapter_ShouldReturnWithoutPurchase() {
            UUID userId = UUID.randomUUID();
            Chapter chapter = createFreeChapter();

            given(chapterRepository.findById(chapter.getId())).willReturn(Optional.of(chapter));

            chapterPurchaseService.purchaseChapter(userId, chapter.getId());

            verify(chapterPurchaseRepository, never()).save(any());
            verify(creditService, never()).useCredit(any(), any());
        }

        @Test
        @DisplayName("이미 구매한 챕터는 멱등성 보장 (중복 구매 안함)")
        void alreadyPurchased_ShouldBeIdempotent() {
            UUID userId = UUID.randomUUID();
            Chapter chapter = createPaidChapter(100);

            given(chapterRepository.findById(chapter.getId())).willReturn(Optional.of(chapter));
            given(chapterPurchaseRepository.existsByUserIdAndChapterId(userId, chapter.getId())).willReturn(true);

            chapterPurchaseService.purchaseChapter(userId, chapter.getId());

            verify(chapterPurchaseRepository, never()).save(any());
            verify(creditService, never()).useCredit(any(), any());
        }

        @Test
        @DisplayName("유료 챕터 정상 구매 - 구매 기록 저장 + 크레딧 차감")
        void paidChapter_ShouldSavePurchaseAndDeductCredit() {
            UUID userId = UUID.randomUUID();
            Chapter chapter = createPaidChapter(100);

            given(chapterRepository.findById(chapter.getId())).willReturn(Optional.of(chapter));
            given(chapterPurchaseRepository.existsByUserIdAndChapterId(userId, chapter.getId())).willReturn(false);
            given(chapterPurchaseRepository.save(any(ChapterPurchase.class))).willAnswer(i -> i.getArgument(0));
            given(creditService.useCredit(eq(userId), any(CreditUseRequest.class)))
                    .willReturn(new CreditResponse(UUID.randomUUID().toString(), 400L, 500L, 100L, null));

            chapterPurchaseService.purchaseChapter(userId, chapter.getId());

            verify(chapterPurchaseRepository).save(any(ChapterPurchase.class));
            verify(creditService).useCredit(eq(userId), any(CreditUseRequest.class));
        }

        @Test
        @DisplayName("동시 구매 시 DataIntegrityViolation 발생하면 멱등성 보장")
        void concurrentPurchase_ShouldHandleGracefully() {
            UUID userId = UUID.randomUUID();
            Chapter chapter = createPaidChapter(100);

            given(chapterRepository.findById(chapter.getId())).willReturn(Optional.of(chapter));
            given(chapterPurchaseRepository.existsByUserIdAndChapterId(userId, chapter.getId())).willReturn(false);
            given(chapterPurchaseRepository.save(any(ChapterPurchase.class)))
                    .willThrow(new DataIntegrityViolationException("Duplicate key"));

            chapterPurchaseService.purchaseChapter(userId, chapter.getId());

            verify(creditService, never()).useCredit(any(), any());
        }

        @Test
        @DisplayName("크레딧 차감 실패 시 구매 기록도 롤백 (트랜잭션)")
        void creditDeductionFails_ShouldRollback() {
            UUID userId = UUID.randomUUID();
            Chapter chapter = createPaidChapter(100);

            given(chapterRepository.findById(chapter.getId())).willReturn(Optional.of(chapter));
            given(chapterPurchaseRepository.existsByUserIdAndChapterId(userId, chapter.getId())).willReturn(false);
            given(chapterPurchaseRepository.save(any(ChapterPurchase.class))).willAnswer(i -> i.getArgument(0));
            given(creditService.useCredit(eq(userId), any(CreditUseRequest.class)))
                    .willThrow(new IllegalStateException("크레딧이 부족합니다"));

            assertThatThrownBy(() -> chapterPurchaseService.purchaseChapter(userId, chapter.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("부족합니다");
        }
    }

    @Nested
    @DisplayName("hasAccess")
    class HasAccess {

        @Test
        @DisplayName("무료 챕터는 항상 접근 가능")
        void freeChapter_ShouldAlwaysHaveAccess() {
            UUID userId = UUID.randomUUID();
            Chapter chapter = createFreeChapter();

            given(chapterRepository.findById(chapter.getId())).willReturn(Optional.of(chapter));

            assertThat(chapterPurchaseService.hasAccess(userId, chapter.getId())).isTrue();
            verify(chapterPurchaseRepository, never()).existsByUserIdAndChapterId(any(), any());
        }

        @Test
        @DisplayName("유료 챕터 - 구매한 경우 접근 가능")
        void paidChapter_Purchased_ShouldHaveAccess() {
            UUID userId = UUID.randomUUID();
            Chapter chapter = createPaidChapter(100);

            given(chapterRepository.findById(chapter.getId())).willReturn(Optional.of(chapter));
            given(chapterPurchaseRepository.existsByUserIdAndChapterId(userId, chapter.getId())).willReturn(true);

            assertThat(chapterPurchaseService.hasAccess(userId, chapter.getId())).isTrue();
        }

        @Test
        @DisplayName("유료 챕터 - 미구매 시 접근 불가")
        void paidChapter_NotPurchased_ShouldNotHaveAccess() {
            UUID userId = UUID.randomUUID();
            Chapter chapter = createPaidChapter(100);

            given(chapterRepository.findById(chapter.getId())).willReturn(Optional.of(chapter));
            given(chapterPurchaseRepository.existsByUserIdAndChapterId(userId, chapter.getId())).willReturn(false);

            assertThat(chapterPurchaseService.hasAccess(userId, chapter.getId())).isFalse();
        }
    }
}
