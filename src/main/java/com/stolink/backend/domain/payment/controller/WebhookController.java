package com.stolink.backend.domain.payment.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.stolink.backend.domain.payment.service.PaymentService;
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

    /**
     * 토스 페이먼츠 웹훅 수신
     */
    @PostMapping("/toss")
    public ResponseEntity<Void> handleTossWebhook(
            @RequestHeader(value = "TossPayments-Signature", required = false) String signature,
            @RequestBody JsonNode payload) {

        String eventType = payload.path("eventType").asText();
        log.info("토스 웹훅 수신: eventType={}", eventType);

        paymentService.handleWebhook(eventType, payload);

        return ResponseEntity.ok().build();
    }
}
