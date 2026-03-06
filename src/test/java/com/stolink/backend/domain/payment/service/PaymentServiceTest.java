package com.stolink.backend.domain.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stolink.backend.domain.payment.client.TossPaymentClient;
import com.stolink.backend.domain.payment.dto.request.PaymentCancelRequest;
import com.stolink.backend.domain.payment.dto.request.PaymentConfirmRequest;
import com.stolink.backend.domain.payment.dto.request.PaymentPrepareRequest;
import com.stolink.backend.domain.payment.dto.response.PaymentPrepareResponse;
import com.stolink.backend.domain.payment.dto.response.PaymentResponse;
import com.stolink.backend.domain.payment.dto.toss.TossPaymentConfirmResponse;
import com.stolink.backend.domain.payment.entity.*;
import com.stolink.backend.domain.payment.exception.PaymentExceptions;
import com.stolink.backend.domain.payment.repository.CreditRepository;
import com.stolink.backend.domain.payment.repository.CreditTransactionRepository;
import com.stolink.backend.domain.payment.repository.PaymentCompensationRepository;
import com.stolink.backend.domain.payment.repository.PaymentRepository;
import com.stolink.backend.domain.payment.repository.PaymentWebhookLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * PaymentService 단위 테스트 (Unit Test)
 *
 * Mockito를 사용하여 외부 의존성(DB, Toss API)을 Mock으로 대체합니다.
 * 비즈니스 로직 및 예외 케이스를 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

        @InjectMocks
        private PaymentService paymentService;

        @Mock
        private PaymentRepository paymentRepository;

        @Mock
        private CreditRepository creditRepository;

        @Mock
        private CreditTransactionRepository creditTransactionRepository;

        @Mock
        private PaymentCompensationRepository compensationRepository;

        @Mock
        private CreditPackageService creditPackageService;

        @Mock
        private TransactionTemplate transactionTemplate;

        @Mock
        private TossPaymentClient tossPaymentClient;

        @Mock
        private PaymentWebhookLogRepository webhookLogRepository;

        // ===================== preparePayment =====================

        @Test
        @DisplayName("preparePayment: 정상적인 요청 시 결제 준비 완료 응답 반환")
        void preparePayment_WhenValid_ShouldReturnResponse() {
                UUID userId = UUID.randomUUID();
                PaymentPrepareRequest request = new PaymentPrepareRequest(1L);
                CreditPackage creditPackage = new CreditPackage(1L, "Test Package", 1000L, 1000L, 100L, false);

                given(creditPackageService.getPackage(1L)).willReturn(creditPackage);
                given(paymentRepository.existsByIdempotencyKey(anyString())).willReturn(false);

                PaymentPrepareResponse response = paymentService.preparePayment(userId, request, null);

                assertThat(response.orderId()).isNotNull();
                assertThat(response.amount()).isEqualTo(1000L);
                verify(paymentRepository).save(any(Payment.class));
        }

        @Test
        @DisplayName("preparePayment: 클라이언트 제공 멱등성 키 중복 시 DuplicatePaymentException 발생")
        void preparePayment_WhenIdempotencyKeyExists_ShouldThrowException() {
                UUID userId = UUID.randomUUID();
                PaymentPrepareRequest request = new PaymentPrepareRequest(1L);
                CreditPackage creditPackage = new CreditPackage(1L, "Test Package", 1000L, 1000L, 100L, false);
                String duplicateKey = "duplicate-key-123";

                given(creditPackageService.getPackage(1L)).willReturn(creditPackage);
                given(paymentRepository.existsByIdempotencyKey("IDEM-" + duplicateKey)).willReturn(true);

                assertThatThrownBy(() -> paymentService.preparePayment(userId, request, duplicateKey))
                                .isInstanceOf(PaymentExceptions.DuplicatePaymentException.class);

                // 중복 시 저장하지 않아야 함
                verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        @DisplayName("preparePayment: 클라이언트 멱등성 키 없을 때 서버가 자동 생성")
        void preparePayment_WithoutClientKey_ShouldGenerateServerKey() {
                UUID userId = UUID.randomUUID();
                PaymentPrepareRequest request = new PaymentPrepareRequest(1L);
                CreditPackage creditPackage = new CreditPackage(1L, "Test Package", 1000L, 1000L, 100L, false);

                given(creditPackageService.getPackage(1L)).willReturn(creditPackage);
                given(paymentRepository.existsByIdempotencyKey(anyString())).willReturn(false);

                PaymentPrepareResponse response = paymentService.preparePayment(userId, request, null);

                // orderId가 있으면 서버 키가 자동 생성된 것
                assertThat(response.orderId()).isNotNull();
                verify(paymentRepository).save(any(Payment.class));
        }

        // ===================== confirmPayment =====================

        @Test
        @DisplayName("confirmPayment: 정상 승인 시 상태 DONE 변경 및 크레딧 지급")
        void confirmPayment_WhenSuccess_ShouldCompletePayment() {
                UUID userId = UUID.randomUUID();
                String orderId = "order-123";
                String paymentKey = "key-123";
                PaymentConfirmRequest request = new PaymentConfirmRequest(orderId, paymentKey, 1000L);

                Payment payment = Payment.builder()
                                .userId(userId).orderId(orderId).orderName("Test Order")
                                .amount(1000L).creditAmount(1100L)
                                .status(PaymentStatus.PENDING).idempotencyKey("idem-123")
                                .build();
                ReflectionTestUtils.setField(payment, "id", UUID.randomUUID());

                TossPaymentConfirmResponse tossResponse = new TossPaymentConfirmResponse(
                                paymentKey, orderId, "Test Order", 1000L, "CARD", "DONE", null, null);

                given(transactionTemplate.execute(any())).willAnswer(invocation -> {
                        TransactionCallback<?> callback = invocation.getArgument(0);
                        return callback.doInTransaction(null);
                });
                lenient().when(paymentRepository.findByOrderIdWithLock(anyString())).thenReturn(Optional.of(payment));
                lenient().when(tossPaymentClient.confirmPayment(anyString(), anyString(), anyLong(), anyString()))
                                .thenReturn(tossResponse);
                lenient().when(creditRepository.findByUserIdWithLock(any(UUID.class)))
                                .thenReturn(Optional.of(Credit.createForUser(userId)));

                PaymentResponse response = paymentService.confirmPayment(userId, request);

                assertThat(response.status()).isEqualTo(PaymentStatus.DONE.name());
        }

        @Test
        @DisplayName("confirmPayment: TX1에서 이미 DONE 상태 반환 시 Toss API 호출 없이 즉시 반환")
        void confirmPayment_WhenAlreadyDone_ShouldSkipTossApiCall() {
                UUID userId = UUID.randomUUID();
                String orderId = "order-123";
                PaymentConfirmRequest request = new PaymentConfirmRequest(orderId, "key-123", 1000L);

                Payment payment = Payment.builder()
                                .userId(userId).orderId(orderId).amount(1000L)
                                .status(PaymentStatus.DONE)
                                .build();
                ReflectionTestUtils.setField(payment, "id", UUID.randomUUID());

                given(transactionTemplate.execute(any())).willReturn(payment);

                PaymentResponse response = paymentService.confirmPayment(userId, request);

                assertThat(response.status()).isEqualTo(PaymentStatus.DONE.name());
                verify(tossPaymentClient, never()).confirmPayment(anyString(), anyString(), anyLong(), anyString());
        }

        @Test
        @DisplayName("confirmPayment: 결제 금액 불일치 시 PaymentAmountMismatchException 발생")
        void confirmPayment_WhenAmountMismatch_ShouldThrowException() {
                UUID userId = UUID.randomUUID();
                String orderId = "order-123";
                PaymentConfirmRequest request = new PaymentConfirmRequest(orderId, "key-123", 500L); // 500 요청

                Payment payment = Payment.builder()
                                .userId(userId).orderId(orderId)
                                .amount(1000L).status(PaymentStatus.PENDING) // 실제는 1000
                                .build();
                ReflectionTestUtils.setField(payment, "id", UUID.randomUUID());

                given(transactionTemplate.execute(any())).willAnswer(invocation -> {
                        TransactionCallback<?> callback = invocation.getArgument(0);
                        return callback.doInTransaction(null);
                });
                lenient().when(paymentRepository.findByOrderIdWithLock(anyString())).thenReturn(Optional.of(payment));

                assertThatThrownBy(() -> paymentService.confirmPayment(userId, request))
                                .isInstanceOf(PaymentExceptions.PaymentAmountMismatchException.class);
        }

        // ===================== cancelPayment =====================

        @Test
        @DisplayName("cancelPayment: 정상적인 전액 취소 시 상태 CANCELED 및 크레딧 차감")
        void cancelPayment_WhenValidFullCancel_ShouldSucceed() {
                UUID userId = UUID.randomUUID();
                UUID paymentId = UUID.randomUUID();
                PaymentCancelRequest request = new PaymentCancelRequest("test-reason", null);

                Payment payment = Payment.builder()
                                .userId(userId).orderId("order-123")
                                .amount(1000L).creditAmount(1000L)
                                .status(PaymentStatus.DONE).paymentKey("key-123")
                                .build();
                ReflectionTestUtils.setField(payment, "id", paymentId);

                Credit credit = Credit.createForUser(userId);
                credit.charge(2000L);

                given(transactionTemplate.execute(any())).willAnswer(invocation -> {
                        TransactionCallback<?> callback = invocation.getArgument(0);
                        return callback.doInTransaction(null);
                });
                lenient().when(paymentRepository.findByIdWithLock(paymentId)).thenReturn(Optional.of(payment));
                lenient().when(creditRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(credit));

                paymentService.cancelPayment(userId, paymentId.toString(), request);

                assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
                assertThat(credit.getBalance()).isEqualTo(1000L);
                verify(tossPaymentClient).cancelPayment(eq("key-123"), eq("test-reason"), eq(1000L), anyString());
                verify(creditTransactionRepository).save(any(CreditTransaction.class));
        }

        @Test
        @DisplayName("cancelPayment: 크레딧 잔액 부족 시 InsufficientCreditException 발생")
        void cancelPayment_WhenInsufficientCredit_ShouldThrowException() {
                UUID userId = UUID.randomUUID();
                UUID paymentId = UUID.randomUUID();
                PaymentCancelRequest request = new PaymentCancelRequest("reason", 1000L);

                Payment payment = Payment.builder()
                                .userId(userId).amount(1000L).creditAmount(1000L)
                                .status(PaymentStatus.DONE)
                                .build();
                ReflectionTestUtils.setField(payment, "id", paymentId);

                Credit credit = Credit.createForUser(userId);
                credit.charge(500L); // 1000원 환불에 500원밖에 없음

                given(transactionTemplate.execute(any())).willAnswer(invocation -> {
                        TransactionCallback<?> callback = invocation.getArgument(0);
                        return callback.doInTransaction(null);
                });
                lenient().when(paymentRepository.findByIdWithLock(paymentId)).thenReturn(Optional.of(payment));
                lenient().when(creditRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(credit));

                assertThatThrownBy(() -> paymentService.cancelPayment(userId, paymentId.toString(), request))
                                .isInstanceOf(PaymentExceptions.InsufficientCreditException.class);
        }

        // ===================== handleWebhook =====================

        @Test
        @DisplayName("handleWebhook: PAYMENT_STATUS_CHANGED(DONE) 수신 시 결제 완료 처리")
        void handleWebhook_WhenPaymentStatusChangedDone_ShouldCompleteProcess() throws Exception {
                String orderId = "order-123";
                String paymentKey = "key-123";
                ObjectMapper mapper = new ObjectMapper();
                JsonNode payload = mapper.readTree(String.format(
                                "{\"eventType\":\"PAYMENT_STATUS_CHANGED\",\"orderId\":\"%s\",\"paymentKey\":\"%s\",\"status\":\"DONE\",\"method\":\"CARD\"}",
                                orderId, paymentKey));

                given(webhookLogRepository.existsByPaymentKeyAndEventTypeAndStatusNot(anyString(), anyString(), any()))
                                .willReturn(false);
                given(transactionTemplate.execute(any())).willAnswer(invocation -> {
                        TransactionCallback<?> callback = invocation.getArgument(0);
                        return callback.doInTransaction(null);
                });

                Payment payment = Payment.builder()
                                .userId(UUID.randomUUID()).orderId(orderId)
                                .amount(1000L).creditAmount(1000L)
                                .status(PaymentStatus.IN_PROGRESS)
                                .build();
                ReflectionTestUtils.setField(payment, "id", UUID.randomUUID());

                lenient().when(paymentRepository.findByOrderIdWithLock(orderId)).thenReturn(Optional.of(payment));
                lenient().when(creditRepository.findByUserIdWithLock(any()))
                                .thenReturn(Optional.of(Credit.createForUser(payment.getUserId())));

                paymentService.handleWebhook("PAYMENT_STATUS_CHANGED", payload);

                assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DONE);
                verify(webhookLogRepository).save(any(PaymentWebhookLog.class));
        }

        @Test
        @DisplayName("handleWebhook: 중복 웹훅 수신 시 처리 건너뜀")
        void handleWebhook_WhenDuplicate_ShouldSkip() throws Exception {
                String paymentKey = "key-123";
                ObjectMapper mapper = new ObjectMapper();
                JsonNode payload = mapper.readTree(
                                "{\"eventType\":\"PAYMENT_STATUS_CHANGED\",\"orderId\":\"o-1\",\"paymentKey\":\""
                                                + paymentKey + "\",\"status\":\"DONE\"}");

                given(webhookLogRepository.existsByPaymentKeyAndEventTypeAndStatusNot(anyString(), anyString(), any()))
                                .willReturn(true); // 이미 처리됨

                paymentService.handleWebhook("PAYMENT_STATUS_CHANGED", payload);

                // 중복이면 로그 저장조차 하지 않아야 함
                verify(transactionTemplate, never()).execute(any());
        }
}
