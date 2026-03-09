package com.stolink.backend.domain.settlement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SettlementRejectRequest(
        @NotBlank(message = "거절 사유는 필수입니다")
        @Size(max = 500, message = "거절 사유는 500자 이내여야 합니다")
        String reason
) {}
