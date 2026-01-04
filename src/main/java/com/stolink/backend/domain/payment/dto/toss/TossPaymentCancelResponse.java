package com.stolink.backend.domain.payment.dto.toss;

import java.util.List;

public record TossPaymentCancelResponse(
    String paymentKey,
    String orderId,
    String status,
    Long totalAmount,
    Long canceledAmount,
    List<CancelDetail> cancels
) {
    public record CancelDetail(
        Long cancelAmount,
        String cancelReason,
        String canceledAt
    ) {}
}
