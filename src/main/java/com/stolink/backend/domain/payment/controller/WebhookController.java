package com.stolink.backend.domain.payment.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stolink.backend.domain.payment.service.PaymentService;
import com.stolink.backend.domain.payment.util.WebhookSignatureValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final PaymentService paymentService;
    private final WebhookSignatureValidator signatureValidator;
    private final ObjectMapper objectMapper;

    /**
     * 토스 페이먼츠 웹훅 수신
     * JWT 대신 HMAC-SHA256 서명으로 인증
     */
    @PostMapping("/toss")
    public ResponseEntity<Void> handleTossWebhook(
            @RequestHeader(value = "TossPayments-Signature", required = false) String signature,
            @RequestBody String rawBody) {

        if (!signatureValidator.isValid(signature, rawBody)) {
            log.warn("웹훅 서명 검증 실패");
            return ResponseEntity.status(401).build();
        }

        try {
            JsonNode payload = objectMapper.readTree(rawBody);
            String eventType = payload.path("eventType").asText();
            log.info("토스 웹훅 수신: eventType={}", eventType);

            paymentService.handleWebhook(eventType, payload);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("웹훅 처리 중 오류 발생", e);
            return ResponseEntity.ok().build();
        }
    }
}
