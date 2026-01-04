package com.stolink.backend.domain.payment.dto.request;

import jakarta.validation.constraints.NotNull;

public record PaymentCancelRequest(
    @NotNull(message = "취소 사유는 필수입니다")
    String cancelReason,

    Long cancelAmount
) {}
