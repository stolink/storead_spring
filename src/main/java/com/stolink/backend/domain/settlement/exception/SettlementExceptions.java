package com.stolink.backend.domain.settlement.exception;

public class SettlementExceptions {

    public static class SettlementNotFoundException extends RuntimeException {
        public SettlementNotFoundException(String message) {
            super(message);
        }
    }

    public static class DuplicateSettlementException extends RuntimeException {
        public DuplicateSettlementException(String message) {
            super(message);
        }
    }

    public static class InvalidSettlementStatusException extends RuntimeException {
        public InvalidSettlementStatusException(String message) {
            super(message);
        }
    }

    public static class InsufficientRevenueException extends RuntimeException {
        public InsufficientRevenueException(String message) {
            super(message);
        }
    }

    public static class DuplicateRevenueException extends RuntimeException {
        public DuplicateRevenueException(String message) {
            super(message);
        }
    }

    public static class AuthorRevenueNotFoundException extends RuntimeException {
        public AuthorRevenueNotFoundException(String message) {
            super(message);
        }
    }
}
