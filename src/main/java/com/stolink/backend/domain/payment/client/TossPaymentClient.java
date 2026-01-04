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
import java.util.Base64;
import java.util.Map;

@Component
@Slf4j
public class TossPaymentClient {

    private static final String TOSS_API_URL = "https://api.tosspayments.com/v1";

    private final WebClient webClient;
    private final String secretKey;

    public TossPaymentClient(
            WebClient.Builder webClientBuilder,
            @Value("${toss.payments.secret-key}") String secretKey) {
        this.webClient = webClientBuilder
            .baseUrl(TOSS_API_URL)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
        this.secretKey = secretKey;
    }

    /**
     * 결제 승인
     */
    public TossPaymentConfirmResponse confirmPayment(String paymentKey, String orderId, Long amount) {
        log.info("토스 결제 승인 요청: paymentKey={}, orderId={}, amount={}", paymentKey, orderId, amount);

        try {
            return webClient.post()
                .uri("/payments/confirm")
                .header(HttpHeaders.AUTHORIZATION, buildAuthorizationHeader())
                .bodyValue(Map.of(
                    "paymentKey", paymentKey,
                    "orderId", orderId,
                    "amount", amount
                ))
                .retrieve()
                .bodyToMono(TossPaymentConfirmResponse.class)
                .block();
        } catch (WebClientResponseException e) {
            log.error("토스 결제 승인 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw parseTossError(e);
        }
    }

    /**
     * 결제 취소
     */
    public TossPaymentCancelResponse cancelPayment(String paymentKey, String cancelReason, Long cancelAmount) {
        log.info("토스 결제 취소 요청: paymentKey={}, reason={}, amount={}", paymentKey, cancelReason, cancelAmount);

        try {
            Map<String, Object> requestBody = cancelAmount != null
                ? Map.of("cancelReason", cancelReason, "cancelAmount", cancelAmount)
                : Map.of("cancelReason", cancelReason);

            return webClient.post()
                .uri("/payments/{paymentKey}/cancel", paymentKey)
                .header(HttpHeaders.AUTHORIZATION, buildAuthorizationHeader())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(TossPaymentCancelResponse.class)
                .block();
        } catch (WebClientResponseException e) {
            log.error("토스 결제 취소 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw parseTossError(e);
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
                .block();
        } catch (WebClientResponseException e) {
            throw parseTossError(e);
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
