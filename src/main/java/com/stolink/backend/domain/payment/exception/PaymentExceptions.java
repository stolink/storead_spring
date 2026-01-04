package com.stolink.backend.domain.payment.exception;

public class PaymentExceptions {

    public static class PaymentNotFoundException extends RuntimeException {
        public PaymentNotFoundException(String message) {
            super(message);
        }
    }

    public static class DuplicatePaymentException extends RuntimeException {
        public DuplicatePaymentException(String message) {
            super(message);
        }
    }

    public static class InvalidPaymentStatusException extends RuntimeException {
        public InvalidPaymentStatusException(String message) {
            super(message);
        }
    }

    public static class PaymentAmountMismatchException extends RuntimeException {
        public PaymentAmountMismatchException(String message) {
            super(message);
        }
    }

    public static class PaymentExpiredException extends RuntimeException {
        public PaymentExpiredException(String message) {
            super(message);
        }
    }

    public static class PaymentNotCancelableException extends RuntimeException {
        public PaymentNotCancelableException(String message) {
            super(message);
        }
    }

    public static class InvalidCancelAmountException extends RuntimeException {
        public InvalidCancelAmountException(String message) {
            super(message);
        }
    }

    public static class UnauthorizedPaymentAccessException extends RuntimeException {
        public UnauthorizedPaymentAccessException(String message) {
            super(message);
        }
    }

    public static class CreditNotFoundException extends RuntimeException {
        public CreditNotFoundException(String message) {
            super(message);
        }
    }

    public static class InsufficientCreditException extends RuntimeException {
        public InsufficientCreditException(String message) {
            super(message);
        }
    }

    public static class CreditPackageNotFoundException extends RuntimeException {
        public CreditPackageNotFoundException(String message) {
            super(message);
        }
    }

    public static class InvalidPaymentStateException extends RuntimeException {
        public InvalidPaymentStateException(String message) {
            super(message);
        }
    }
}
