package com.stolink.backend.domain.payment.dto.toss;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TossPaymentConfirmResponse(
    String paymentKey,
    String orderId,
    String orderName,
    Long totalAmount,
    String method,
    String status,
    String requestedAt,
    String approvedAt
) {}
