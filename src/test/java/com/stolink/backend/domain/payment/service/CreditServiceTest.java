package com.stolink.backend.domain.payment.service;

import com.stolink.backend.domain.payment.dto.request.CreditUseRequest;
import com.stolink.backend.domain.payment.dto.response.CreditCheckResponse;
import com.stolink.backend.domain.payment.dto.response.CreditResponse;
import com.stolink.backend.domain.payment.entity.Credit;
import com.stolink.backend.domain.payment.entity.CreditTransaction;
import com.stolink.backend.domain.payment.exception.PaymentExceptions;
import com.stolink.backend.domain.payment.repository.CreditRepository;
import com.stolink.backend.domain.payment.repository.CreditTransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditServiceTest {

    @InjectMocks
    private CreditService creditService;

    @Mock
    private CreditRepository creditRepository;

    @Mock
    private CreditTransactionRepository creditTransactionRepository;

    @Test
    @DisplayName("getCredit: 사용자가 존재하면 해당 크레딧 반환")
    void getCredit_WhenExists_ShouldReturnCredit() {
        // given
        UUID userId = UUID.randomUUID();
        Credit credit = Credit.createForUser(userId);
        ReflectionTestUtils.setField(credit, "id", UUID.randomUUID());
        credit.charge(1000L);

        given(creditRepository.findByUserId(userId)).willReturn(Optional.of(credit));

        // when
        CreditResponse response = creditService.getCredit(userId);

        // then
        assertThat(response.balance()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("getCredit: 사용자가 없으면 새 크레딧 생성하여 반환")
    void getCredit_WhenNotExists_ShouldCreateNewCredit() {
        // given
        UUID userId = UUID.randomUUID();
        given(creditRepository.findByUserId(userId)).willReturn(Optional.empty());
        given(creditRepository.save(any(Credit.class))).willAnswer(invocation -> {
            Credit c = invocation.getArgument(0);
            ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
            return c;
        });

        // when
        CreditResponse response = creditService.getCredit(userId);

        // then
        assertThat(response.balance()).isEqualTo(0L);
        verify(creditRepository).save(any(Credit.class));
    }

    @Test
    @DisplayName("useCredit: 잔액 충분 시 차감 성공")
    void useCredit_WhenBalanceSufficient_ShouldSucceed() {
        // given
        UUID userId = UUID.randomUUID();
        Credit credit = Credit.createForUser(userId);
        ReflectionTestUtils.setField(credit, "id", UUID.randomUUID());
        credit.charge(1000L); // 1000원 보유

        CreditUseRequest request = new CreditUseRequest(500L, "Test", null, null);

        given(creditRepository.findByUserIdWithLock(userId)).willReturn(Optional.of(credit));

        // when
        CreditResponse response = creditService.useCredit(userId, request);

        // then
        assertThat(response.balance()).isEqualTo(500L);
        verify(creditTransactionRepository).save(any(CreditTransaction.class));
    }

    @Test
    @DisplayName("useCredit: 잔액 부족 시 예외 발생")
    void useCredit_WhenBalanceInsufficient_ShouldThrowException() {
        // given
        UUID userId = UUID.randomUUID();
        Credit credit = Credit.createForUser(userId);
        ReflectionTestUtils.setField(credit, "id", UUID.randomUUID());
        credit.charge(100L); // 100원 보유

        CreditUseRequest request = new CreditUseRequest(500L, "Test", null, null); // 500원 사용 시도

        given(creditRepository.findByUserIdWithLock(userId)).willReturn(Optional.of(credit));

        // when & then
        assertThatThrownBy(() -> creditService.useCredit(userId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("부족합니다");
    }

    @Test
    @DisplayName("checkCredit: 잔액 충분 시 AVAILABLE 반환")
    void checkCredit_WhenSufficient_ShouldReturnAvailable() {
        // given
        UUID userId = UUID.randomUUID();
        Credit credit = Credit.createForUser(userId);
        ReflectionTestUtils.setField(credit, "id", UUID.randomUUID());
        credit.charge(1000L);

        given(creditRepository.findByUserId(userId)).willReturn(Optional.of(credit));

        // when
        CreditCheckResponse response = creditService.checkCredit(userId, 500L);

        // then
        assertThat(response.available()).isTrue();
        assertThat(response.currentBalance()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("checkCredit: 잔액 부족 시 INSUFFICIENT 반환")
    void checkCredit_WhenInsufficient_ShouldReturnInsufficient() {
        // given
        UUID userId = UUID.randomUUID();
        Credit credit = Credit.createForUser(userId);
        ReflectionTestUtils.setField(credit, "id", UUID.randomUUID());
        credit.charge(100L);

        given(creditRepository.findByUserId(userId)).willReturn(Optional.of(credit));

        // when
        CreditCheckResponse response = creditService.checkCredit(userId, 500L);

        // then
        assertThat(response.available()).isFalse();
        assertThat(response.currentBalance()).isEqualTo(100L);
    }

    @Test
    @DisplayName("useCredit: 레이스 컨디션 발생 시 재시도하여 성공")
    void useCredit_WhenRaceCondition_ShouldRetryAndSucceed() {
        // given
        UUID userId = UUID.randomUUID();
        Credit credit = Credit.createForUser(userId);
        ReflectionTestUtils.setField(credit, "id", UUID.randomUUID());
        credit.charge(1000L);

        // 첫 번째 조회 시 empty, 저장 시 DataIntegrityViolationException 발생, 다시 조회 시 성공
        given(creditRepository.findByUserIdWithLock(userId))
                .willReturn(Optional.empty()) // 첫 시도
                .willReturn(Optional.of(credit)); // 재시도

        given(creditRepository.save(any(Credit.class)))
                .willThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate key")) // INSERT 실패
                .willReturn(credit); // UPDATE 성공

        CreditUseRequest request = new CreditUseRequest(500L, "Test", null, null);

        // when
        CreditResponse response = creditService.useCredit(userId, request);

        // then
        assertThat(response.balance()).isEqualTo(500L);
        verify(creditRepository, times(2)).findByUserIdWithLock(userId);
    }

    @Test
    @DisplayName("canUseCredit: 잔액 확인 로직 검증")
    void canUseCredit_Tests() {
        // given
        UUID userId = UUID.randomUUID();
        Credit credit = Credit.createForUser(userId);
        credit.charge(100L);

        given(creditRepository.findByUserId(userId)).willReturn(Optional.of(credit)).willReturn(Optional.empty());

        // when & then
        assertThat(creditService.canUseCredit(userId, 50L)).isTrue();
        assertThat(creditService.canUseCredit(userId, 150L)).isFalse();
        assertThat(creditService.canUseCredit(UUID.randomUUID(), 50L)).isFalse(); // Not found case
    }

    @Test
    @DisplayName("getTransactions: 거래 내역 페이징 조회")
    void getTransactions_ShouldReturnPage() {
        // given
        UUID userId = UUID.randomUUID();
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        given(creditTransactionRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), eq(pageable)))
                .willReturn(org.springframework.data.domain.Page.empty());

        // when
        creditService.getTransactions(userId, pageable);

        // then
        verify(creditTransactionRepository).findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}
