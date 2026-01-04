package com.stolink.backend.domain.payment.dto.response;

import com.stolink.backend.domain.payment.entity.Payment;

import java.time.LocalDateTime;

public record PaymentResponse(
    String id,
    String orderId,
    String orderName,
    Long amount,
    Long creditAmount,
    String paymentKey,
    String paymentMethod,
    String status,
    Long canceledAmount,
    String cancelReason,
    LocalDateTime requestedAt,
    LocalDateTime approvedAt,
    LocalDateTime canceledAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
            payment.getId().toString(),
            payment.getOrderId(),
            payment.getOrderName(),
            payment.getAmount(),
            payment.getCreditAmount(),
            payment.getPaymentKey(),
            payment.getPaymentMethod(),
            payment.getStatus().name(),
            payment.getCanceledAmount(),
            payment.getCancelReason(),
            payment.getRequestedAt(),
            payment.getApprovedAt(),
            payment.getCanceledAt()
        );
    }
}
