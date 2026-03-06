package com.stolink.backend.domain.payment.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
@Slf4j
public class WebhookSignatureValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final String webhookSecret;

    public WebhookSignatureValidator(
            @Value("${toss.payments.webhook-secret}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    /**
     * 토스 페이먼츠 웹훅 서명 검증
     * HMAC-SHA256(webhookSecret, rawBody) == signature 헤더 값
     */
    public boolean isValid(String signature, String rawBody) {
        if (signature == null || signature.isBlank()) {
            log.warn("웹훅 서명 헤더가 비어있습니다.");
            return false;
        }
        if (rawBody == null || rawBody.isBlank()) {
            log.warn("웹훅 요청 본문이 비어있습니다.");
            return false;
        }

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKey);

            byte[] hash = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String computed = Base64.getEncoder().encodeToString(hash);

            return MessageDigest.isEqual(
                    computed.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("HMAC-SHA256 서명 검증 중 오류 발생", e);
            return false;
        }
    }
}
