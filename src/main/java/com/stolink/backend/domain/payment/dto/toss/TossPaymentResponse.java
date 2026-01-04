package com.stolink.backend.domain.payment.dto.toss;

public record TossPaymentResponse(
    String paymentKey,
    String orderId,
    String orderName,
    String method,
    String status,
    Long totalAmount,
    String requestedAt,
    String approvedAt
) {}
