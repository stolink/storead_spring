package com.stolink.backend.domain.payment.dto.response;

/**
 * 크레딧 사용 가능 여부 확인 응답
 */
public record CreditCheckResponse(
    boolean available,           // 사용 가능 여부
    Long currentBalance,         // 현재 잔액
    Long requestedAmount,        // 요청 금액
    Long afterBalance,           // 사용 후 잔액 (사용 가능할 때만)
    Long shortfall               // 부족 금액 (부족할 때만)
) {

    /**
     * 사용 가능한 경우
     */
    public static CreditCheckResponse available(Long currentBalance, Long requestedAmount) {
        return new CreditCheckResponse(
            true,
            currentBalance,
            requestedAmount,
            currentBalance - requestedAmount,
            null
        );
    }

    /**
     * 사용 불가능한 경우 (잔액 부족)
     */
    public static CreditCheckResponse insufficient(Long currentBalance, Long requestedAmount) {
        return new CreditCheckResponse(
            false,
            currentBalance,
            requestedAmount,
            null,
            requestedAmount - currentBalance
        );
    }
}
