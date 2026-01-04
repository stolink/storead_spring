package com.stolink.backend.domain.payment.dto.response;

public record PaymentPrepareResponse(
    String orderId,
    String orderName,
    Long amount,
    Long creditAmount,
    String customerKey,
    String successUrl,
    String failUrl
) {}
