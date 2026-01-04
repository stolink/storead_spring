package com.stolink.backend.domain.payment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreditUseRequest(
    @NotNull(message = "사용 금액은 필수입니다")
    @Min(value = 1, message = "사용 금액은 1 이상이어야 합니다")
    Long amount,

    @NotNull(message = "사용 설명은 필수입니다")
    String description,

    String referenceType,
    String referenceId
) {}
