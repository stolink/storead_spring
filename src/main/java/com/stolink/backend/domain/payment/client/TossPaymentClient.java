package com.stolink.backend.domain.payment.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.stolink.backend.domain.payment.dto.toss.TossPaymentCancelResponse;
import com.stolink.backend.domain.payment.dto.toss.TossPaymentConfirmResponse;
import com.stolink.backend.domain.payment.dto.toss.TossPaymentResponse;
import com.stolink.backend.domain.payment.exception.TossPaymentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Component
@Slf4j
public class TossPaymentClient {

    private final WebClient webClient;
    private final String secretKey;

    public TossPaymentClient(
            WebClient.Builder webClientBuilder,
            @Value("${toss.payments.secret-key}") String secretKey,
            @Value("${toss.payments.api-url:https://api.tosspayments.com/v1}") String apiUrl) {
        this.webClient = webClientBuilder
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.secretKey = secretKey;
    }

    /**
     * 결제 승인
     */
    public TossPaymentConfirmResponse confirmPayment(String paymentKey, String orderId, Long amount,
            String idempotencyKey) {
        log.info("토스 결제 승인 요청: paymentKey={}, orderId={}, amount={}, idempotencyKey={}", paymentKey, orderId, amount,
                idempotencyKey);

        try {
            return webClient.post()
                    .uri("/payments/confirm")
                    .header(HttpHeaders.AUTHORIZATION, buildAuthorizationHeader())
                    .header("Idempotency-Key", idempotencyKey)
                    .bodyValue(Map.of(
                            "paymentKey", paymentKey,
                            "orderId", orderId,
                            "amount", amount))
                    .retrieve()
                    .bodyToMono(TossPaymentConfirmResponse.class)
                    .block(Duration.ofSeconds(5));
        } catch (WebClientResponseException e) {
            log.error("토스 결제 승인 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw parseTossError(e);
        } catch (Exception e) {
            log.error("토스 결제 승인 중 시스템 오류: {}", e.getMessage());
            throw new TossPaymentException("SYSTEM_ERROR", "결제 승인 요청 중 오류가 발생했습니다.", 500);
        }
    }

    /**
     * 결제 취소
     */
    public TossPaymentCancelResponse cancelPayment(String paymentKey, String cancelReason, Long cancelAmount,
            String idempotencyKey) {
        log.info("토스 결제 취소 요청: paymentKey={}, reason={}, amount={}, idempotencyKey={}", paymentKey, cancelReason,
                cancelAmount, idempotencyKey);

        try {
            Map<String, Object> requestBody = cancelAmount != null
                    ? Map.of("cancelReason", cancelReason, "cancelAmount", cancelAmount)
                    : Map.of("cancelReason", cancelReason);

            return webClient.post()
                    .uri("/payments/{paymentKey}/cancel", paymentKey)
                    .header(HttpHeaders.AUTHORIZATION, buildAuthorizationHeader())
                    .header("Idempotency-Key", idempotencyKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(TossPaymentCancelResponse.class)
                    .block(Duration.ofSeconds(5));
        } catch (WebClientResponseException e) {
            log.error("토스 결제 취소 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw parseTossError(e);
        } catch (Exception e) {
            log.error("토스 결제 취소 중 시스템 오류: {}", e.getMessage());
            throw new TossPaymentException("SYSTEM_ERROR", "결제 취소 요청 중 오류가 발생했습니다.", 500);
        }
    }

    /**
     * 결제 조회
     */
    public TossPaymentResponse getPayment(String paymentKey) {
        try {
            return webClient.get()
                    .uri("/payments/{paymentKey}", paymentKey)
                    .header(HttpHeaders.AUTHORIZATION, buildAuthorizationHeader())
                    .retrieve()
                    .bodyToMono(TossPaymentResponse.class)
                    .block(Duration.ofSeconds(5));
        } catch (WebClientResponseException e) {
            throw parseTossError(e);
        } catch (Exception e) {
            throw new TossPaymentException("SYSTEM_ERROR", "결제 조회 요청 중 오류가 발생했습니다.", 500);
        }
    }

    private String buildAuthorizationHeader() {
        String credentials = secretKey + ":";
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }

    private TossPaymentException parseTossError(WebClientResponseException e) {
        try {
            JsonNode errorBody = e.getResponseBodyAs(JsonNode.class);
            String code = errorBody != null && errorBody.has("code")
                    ? errorBody.get("code").asText("UNKNOWN_ERROR")
                    : "UNKNOWN_ERROR";
            String message = errorBody != null && errorBody.has("message")
                    ? errorBody.get("message").asText("알 수 없는 오류가 발생했습니다.")
                    : "알 수 없는 오류가 발생했습니다.";
            return new TossPaymentException(code, message, e.getStatusCode().value());
        } catch (Exception parseError) {
            return new TossPaymentException("PARSE_ERROR", e.getMessage(), e.getStatusCode().value());
        }
    }
}
