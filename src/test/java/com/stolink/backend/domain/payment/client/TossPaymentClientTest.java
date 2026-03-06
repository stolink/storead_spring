package com.stolink.backend.domain.payment.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stolink.backend.domain.payment.dto.toss.TossPaymentCancelResponse;
import com.stolink.backend.domain.payment.dto.toss.TossPaymentConfirmResponse;
import com.stolink.backend.domain.payment.dto.toss.TossPaymentResponse;
import com.stolink.backend.domain.payment.exception.TossPaymentException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TossPaymentClientTest {

        private MockWebServer mockWebServer;
        private TossPaymentClient tossPaymentClient;
        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() throws IOException {
                mockWebServer = new MockWebServer();
                mockWebServer.start();
                String baseUrl = mockWebServer.url("/v1").toString();
                WebClient.Builder builder = WebClient.builder();
                tossPaymentClient = new TossPaymentClient(builder, "test_secret_key", baseUrl);
                objectMapper = new ObjectMapper();
        }

        @AfterEach
        void tearDown() throws IOException {
                mockWebServer.shutdown();
        }

        @Test
        @DisplayName("결제 승인 성공 - 정상적인 응답을 반환한다")
        void confirmPayment_Success() throws JsonProcessingException {
                // given
                String paymentKey = "test_payment_key";
                String orderId = "test_order_id";
                Long amount = 1000L;
                String idempotencyKey = "test_idempotency_key";

                TossPaymentConfirmResponse mockResponse = new TossPaymentConfirmResponse(
                                paymentKey, orderId, "test_order_name", amount, "카드", "DONE", "2023-01-01T00:00:00",
                                "2023-01-01T00:00:01");

                mockWebServer.enqueue(new MockResponse()
                                .setResponseCode(HttpStatus.OK.value())
                                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .setBody(objectMapper.writeValueAsString(mockResponse)));

                // when
                TossPaymentConfirmResponse response = tossPaymentClient.confirmPayment(paymentKey, orderId, amount,
                                idempotencyKey);

                // then
                assertThat(response).isNotNull();
                assertThat(response.paymentKey()).isEqualTo(paymentKey);
        }

        @Test
        @DisplayName("결제 승인 실패 - WebClientResponseException 발생 시 TossPaymentException으로 변환한다")
        void confirmPayment_Fail() throws JsonProcessingException {
                // given
                String paymentKey = "test_payment_key";
                String orderId = "test_order_id";
                Long amount = 1000L;
                String idempotencyKey = "test_idempotency_key";

                Map<String, String> errorBody = Map.of(
                                "code", "NOT_FOUND_PAYMENT",
                                "message", "존재하지 않는 결제입니다.");

                mockWebServer.enqueue(new MockResponse()
                                .setResponseCode(HttpStatus.BAD_REQUEST.value())
                                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .setBody(objectMapper.writeValueAsString(errorBody)));

                // when & then
                assertThatThrownBy(() -> tossPaymentClient.confirmPayment(paymentKey, orderId, amount, idempotencyKey))
                                .isInstanceOf(TossPaymentException.class)
                                .satisfies(e -> {
                                        TossPaymentException tossEx = (TossPaymentException) e;
                                        assertThat(tossEx.getErrorCode()).isEqualTo("NOT_FOUND_PAYMENT");
                                        assertThat(tossEx.getStatusCode()).isEqualTo(400);
                                });
        }

        @Test
        @DisplayName("결제 승인 시 네트워크 장애 등 시스템 오류 발생")
        void confirmPayment_SystemError() {
                // given
                String paymentKey = "test_payment_key";
                String orderId = "test_order_id";
                Long amount = 1000L;
                String idempotencyKey = "test_idempotency_key";

                // 비정상적인 응답
                mockWebServer.enqueue(new MockResponse()
                                .setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START));

                // when & then
                assertThatThrownBy(() -> tossPaymentClient.confirmPayment(paymentKey, orderId, amount, idempotencyKey))
                                .isInstanceOf(TossPaymentException.class)
                                .satisfies(e -> {
                                        TossPaymentException tossEx = (TossPaymentException) e;
                                        assertThat(tossEx.getErrorCode()).isEqualTo("SYSTEM_ERROR");
                                        assertThat(tossEx.getStatusCode()).isEqualTo(500);
                                });
        }

        @Test
        @DisplayName("결제 취소 성공 - 전액 취소")
        void cancelPayment_Success_Full() throws JsonProcessingException {
                // given
                String paymentKey = "test_payment_key";
                String cancelReason = "고객 변심";
                String idempotencyKey = "test_idempotency_key";

                TossPaymentCancelResponse mockResponse = new TossPaymentCancelResponse(
                                paymentKey, "test_order_id", "CANCELED", 1000L, 1000L, Collections.emptyList());
                mockWebServer.enqueue(new MockResponse()
                                .setResponseCode(HttpStatus.OK.value())
                                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .setBody(objectMapper.writeValueAsString(mockResponse)));

                // when
                TossPaymentCancelResponse response = tossPaymentClient.cancelPayment(paymentKey, cancelReason, null,
                                idempotencyKey);

                // then
                assertThat(response).isNotNull();
                assertThat(response.status()).isEqualTo("CANCELED");
        }

        @Test
        @DisplayName("결제 취소 성공 - 부분 취소")
        void cancelPayment_Success_Partial() throws JsonProcessingException {
                // given
                String paymentKey = "test_payment_key";
                String cancelReason = "부분 환불";
                Long cancelAmount = 500L;
                String idempotencyKey = "test_idempotency_key";

                TossPaymentCancelResponse mockResponse = new TossPaymentCancelResponse(
                                paymentKey, "test_order_id", "PARTIAL_CANCELED", 1000L, 500L, Collections.emptyList());
                mockWebServer.enqueue(new MockResponse()
                                .setResponseCode(HttpStatus.OK.value())
                                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .setBody(objectMapper.writeValueAsString(mockResponse)));

                // when
                TossPaymentCancelResponse response = tossPaymentClient.cancelPayment(paymentKey, cancelReason,
                                cancelAmount,
                                idempotencyKey);

                // then
                assertThat(response).isNotNull();
                assertThat(response.status()).isEqualTo("PARTIAL_CANCELED");
        }

        @Test
        @DisplayName("결제 취소 실패 - 토스 API 오류")
        void cancelPayment_Fail() throws JsonProcessingException {
                // given
                String paymentKey = "test_payment_key";
                String cancelReason = "고객 변심";
                String idempotencyKey = "test_idempotency_key";

                Map<String, String> errorBody = Map.of(
                                "code", "ALREADY_CANCELED_PAYMENT",
                                "message", "이미 취소된 결제입니다.");

                mockWebServer.enqueue(new MockResponse()
                                .setResponseCode(HttpStatus.BAD_REQUEST.value())
                                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .setBody(objectMapper.writeValueAsString(errorBody)));

                // when & then
                assertThatThrownBy(
                                () -> tossPaymentClient.cancelPayment(paymentKey, cancelReason, null, idempotencyKey))
                                .isInstanceOf(TossPaymentException.class)
                                .satisfies(e -> {
                                        TossPaymentException tossEx = (TossPaymentException) e;
                                        assertThat(tossEx.getErrorCode()).isEqualTo("ALREADY_CANCELED_PAYMENT");
                                        assertThat(tossEx.getStatusCode()).isEqualTo(400);
                                });
        }

        @Test
        @DisplayName("결제 조회 성공")
        void getPayment_Success() throws JsonProcessingException {
                // given
                String paymentKey = "test_payment_key";

                TossPaymentResponse mockResponse = new TossPaymentResponse(
                                paymentKey, "test_order_id", "test_order_name", "카드", "DONE", 1000L,
                                "2023-01-01T00:00:00",
                                "2023-01-01T00:00:01");
                mockWebServer.enqueue(new MockResponse()
                                .setResponseCode(HttpStatus.OK.value())
                                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .setBody(objectMapper.writeValueAsString(mockResponse)));

                // when
                TossPaymentResponse response = tossPaymentClient.getPayment(paymentKey);

                // then
                assertThat(response).isNotNull();
                assertThat(response.paymentKey()).isEqualTo(paymentKey);
        }

        @Test
        @DisplayName("결제 조회 실패 - 해당하는 결제 없음")
        void getPayment_Fail() throws JsonProcessingException {
                // given
                String paymentKey = "test_payment_key";

                Map<String, String> errorBody = Map.of(
                                "code", "NOT_FOUND",
                                "message", "존재하지 않는 결제 정보입니다.");

                mockWebServer.enqueue(new MockResponse()
                                .setResponseCode(HttpStatus.NOT_FOUND.value())
                                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .setBody(objectMapper.writeValueAsString(errorBody)));

                // when & then
                assertThatThrownBy(() -> tossPaymentClient.getPayment(paymentKey))
                                .isInstanceOf(TossPaymentException.class)
                                .satisfies(e -> {
                                        TossPaymentException tossEx = (TossPaymentException) e;
                                        assertThat(tossEx.getErrorCode()).isEqualTo("NOT_FOUND");
                                        assertThat(tossEx.getStatusCode()).isEqualTo(404);
                                });
        }

        @Test
        @DisplayName("토스 에러 파싱 실패 시 기본 오류 처리")
        void tossError_ParsingFail() {
                // given
                String paymentKey = "test_payment_key";

                // JSON 형태가 아닌 응답 문자열
                mockWebServer.enqueue(new MockResponse()
                                .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .setBody("<html>500 Internal Server Error</html>"));

                // when & then
                assertThatThrownBy(() -> tossPaymentClient.getPayment(paymentKey))
                                .isInstanceOf(TossPaymentException.class)
                                .satisfies(e -> {
                                        TossPaymentException tossEx = (TossPaymentException) e;
                                        assertThat(tossEx.getErrorCode()).isEqualTo("PARSE_ERROR");
                                        assertThat(tossEx.getStatusCode()).isEqualTo(500);
                                });
        }
}
