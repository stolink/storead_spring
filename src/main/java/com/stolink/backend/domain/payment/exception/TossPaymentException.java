package com.stolink.backend.domain.payment.exception;

import lombok.Getter;

@Getter
public class TossPaymentException extends RuntimeException {
    private final String errorCode;
    private final int statusCode;

    public TossPaymentException(String errorCode, String message, int statusCode) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }
}
