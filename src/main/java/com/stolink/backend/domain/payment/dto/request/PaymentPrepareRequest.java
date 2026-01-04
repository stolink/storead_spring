package com.stolink.backend.domain.payment.dto.request;

import jakarta.validation.constraints.NotNull;

public record PaymentPrepareRequest(
    @NotNull(message = "크레딧 패키지 ID는 필수입니다")
    Long packageId
) {}
