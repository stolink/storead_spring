package com.stolink.backend.domain.payment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PaymentCancelRequest(
        @NotNull(message = "취소 사유는 필수입니다") String cancelReason,

        @Min(value = 1, message = "취소 금액은 1원 이상이어야 합니다") Long cancelAmount) {
}
