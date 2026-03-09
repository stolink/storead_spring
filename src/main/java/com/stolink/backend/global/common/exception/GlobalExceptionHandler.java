package com.stolink.backend.global.common.exception;

import com.stolink.backend.global.common.dto.ApiResponse;
import com.stolink.backend.global.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.hibernate.exception.SQLGrammarException;
import org.springframework.jdbc.BadSqlGrammarException;

import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(UnauthorizedException.class)
        public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex,
                        HttpServletRequest request) {
                log.error("Unauthorized: {} [URI: {}]", ex.getMessage(), request.getRequestURI());
                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.UNAUTHORIZED)
                                                .message(ex.getMessage())
                                                .build());
        }

        @ExceptionHandler(MissingRequestHeaderException.class)
        public ResponseEntity<ApiResponse<Void>> handleMissingRequestHeader(MissingRequestHeaderException ex,
                        HttpServletRequest request) {
                log.error("Missing request header: {} [URI: {}]", ex.getMessage(), request.getRequestURI());
                String message = "X-User-Id".equals(ex.getHeaderName())
                                ? "로그인이 필요합니다."
                                : String.format("필수 헤더 '%s'가 누락되었습니다.", ex.getHeaderName());
                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.UNAUTHORIZED)
                                                .message(message)
                                                .build());
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
                log.error("Access denied: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.FORBIDDEN)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.FORBIDDEN)
                                                .message(ex.getMessage())
                                                .build());
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
                log.error("Resource not found: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.NOT_FOUND)
                                                .message(ex.getMessage())
                                                .build());
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
                String message = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(FieldError::getDefaultMessage)
                                .collect(Collectors.joining(", "));

                log.error("Validation error: {}", message);
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.BAD_REQUEST)
                                                .message(message)
                                                .build());
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
                log.error("Illegal argument: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.BAD_REQUEST)
                                                .message(ex.getMessage())
                                                .build());
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(
                        MethodArgumentTypeMismatchException ex) {
                log.error("Method argument type mismatch: {}", ex.getMessage());
                String message = String.format("파라미터 '%s'의 값이 잘못되었습니다: %s", ex.getName(), ex.getValue());
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.BAD_REQUEST)
                                                .message(message)
                                                .build());
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(
                        HttpMessageNotReadableException ex) {
                log.error("Message not readable: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.BAD_REQUEST)
                                                .message("잘못된 요청 형식입니다. JSON 포맷을 확인해주세요.")
                                                .build());
        }

        @ExceptionHandler(com.stolink.backend.domain.draft.exception.DraftExpiredException.class)
        public ResponseEntity<ApiResponse<Void>> handleDraftExpired(
                        com.stolink.backend.domain.draft.exception.DraftExpiredException ex) {
                log.error("Draft expired: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.GONE)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.GONE)
                                                .message(ex.getMessage())
                                                .build());
        }

        @ExceptionHandler(com.stolink.backend.domain.community.exception.DuplicateChapterException.class)
        public ResponseEntity<ApiResponse<Void>> handleDuplicateChapter(
                        com.stolink.backend.domain.community.exception.DuplicateChapterException ex) {
                log.error("Duplicate chapter detected: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.CONFLICT)
                                                .message(ex.getMessage())
                                                .build());
        }

        @ExceptionHandler(com.stolink.backend.domain.payment.exception.PaymentExceptions.PaymentNotCancelableException.class)
        public ResponseEntity<ApiResponse<Void>> handlePaymentNotCancelable(
                        com.stolink.backend.domain.payment.exception.PaymentExceptions.PaymentNotCancelableException ex) {
                log.error("Payment not cancelable: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.CONFLICT)
                                                .message(ex.getMessage())
                                                .build());
        }

        @ExceptionHandler(com.stolink.backend.domain.payment.exception.PaymentExceptions.InvalidCancelAmountException.class)
        public ResponseEntity<ApiResponse<Void>> handleInvalidCancelAmount(
                        com.stolink.backend.domain.payment.exception.PaymentExceptions.InvalidCancelAmountException ex) {
                log.error("Invalid cancel amount: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.BAD_REQUEST)
                                                .message(ex.getMessage())
                                                .build());
        }

        @ExceptionHandler(com.stolink.backend.domain.payment.exception.PaymentExceptions.InvalidPaymentStateException.class)
        public ResponseEntity<ApiResponse<Void>> handleInvalidPaymentState(
                        com.stolink.backend.domain.payment.exception.PaymentExceptions.InvalidPaymentStateException ex) {
                log.error("Invalid payment state: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.BAD_REQUEST)
                                                .message(ex.getMessage())
                                                .build());
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
                log.error("Internal server error", ex);
                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                .message("서버 내부 오류가 발생했습니다.")
                                                .build());
        }

        @ExceptionHandler(com.stolink.backend.domain.payment.exception.PaymentExceptions.PaymentNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handlePaymentNotFound(
                        com.stolink.backend.domain.payment.exception.PaymentExceptions.PaymentNotFoundException ex) {
                log.error("Payment not found: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.NOT_FOUND)
                                                .message(ex.getMessage())
                                                .build());
        }

        @ExceptionHandler(com.stolink.backend.domain.payment.exception.PaymentExceptions.DuplicatePaymentException.class)
        public ResponseEntity<ApiResponse<Void>> handleDuplicatePayment(
                        com.stolink.backend.domain.payment.exception.PaymentExceptions.DuplicatePaymentException ex) {
                log.error("Duplicate payment: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.CONFLICT)
                                                .message(ex.getMessage())
                                                .build());
        }

        @ExceptionHandler(com.stolink.backend.domain.payment.exception.PaymentExceptions.InvalidPaymentStatusException.class)
        public ResponseEntity<ApiResponse<Void>> handleInvalidPaymentStatus(
                        com.stolink.backend.domain.payment.exception.PaymentExceptions.InvalidPaymentStatusException ex) {
                log.error("Invalid payment status: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.BAD_REQUEST)
                                                .message(ex.getMessage())
                                                .build());
        }

        @ExceptionHandler(com.stolink.backend.domain.payment.exception.PaymentExceptions.PaymentAmountMismatchException.class)
        public ResponseEntity<ApiResponse<Void>> handlePaymentAmountMismatch(
                        com.stolink.backend.domain.payment.exception.PaymentExceptions.PaymentAmountMismatchException ex) {
                log.error("Payment amount mismatch: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.BAD_REQUEST)
                                                .message(ex.getMessage())
                                                .build());
        }

        @ExceptionHandler(com.stolink.backend.domain.payment.exception.PaymentExceptions.PaymentExpiredException.class)
        public ResponseEntity<ApiResponse<Void>> handlePaymentExpired(
                        com.stolink.backend.domain.payment.exception.PaymentExceptions.PaymentExpiredException ex) {
                log.error("Payment expired: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.GONE)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.GONE)
                                                .message(ex.getMessage())
                                                .build());
        }

        @ExceptionHandler(com.stolink.backend.domain.payment.exception.PaymentExceptions.InsufficientCreditException.class)
        public ResponseEntity<ApiResponse<Void>> handleInsufficientCredit(
                        com.stolink.backend.domain.payment.exception.PaymentExceptions.InsufficientCreditException ex) {
                log.error("Insufficient credit: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.PAYMENT_REQUIRED)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.PAYMENT_REQUIRED)
                                                .message(ex.getMessage())
                                                .build());
        }

        @ExceptionHandler(com.stolink.backend.domain.payment.exception.PaymentExceptions.UnauthorizedPaymentAccessException.class)
        public ResponseEntity<ApiResponse<Void>> handleUnauthorizedPaymentAccess(
                        com.stolink.backend.domain.payment.exception.PaymentExceptions.UnauthorizedPaymentAccessException ex) {
                log.error("Unauthorized payment access: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.FORBIDDEN)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.FORBIDDEN)
                                                .message(ex.getMessage())
                                                .build());
        }

        @ExceptionHandler(com.stolink.backend.domain.payment.exception.PaymentExceptions.CreditNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleCreditNotFound(
                        com.stolink.backend.domain.payment.exception.PaymentExceptions.CreditNotFoundException ex) {
                log.error("Credit not found: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.BAD_REQUEST)
                                                .message(ex.getMessage())
                                                .build());
        }

        @ExceptionHandler(com.stolink.backend.domain.payment.exception.PaymentExceptions.CreditPackageNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleCreditPackageNotFound(
                        com.stolink.backend.domain.payment.exception.PaymentExceptions.CreditPackageNotFoundException ex) {
                log.error("Credit package not found: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.NOT_FOUND)
                                                .message(ex.getMessage())
                                                .build());
        }

        @ExceptionHandler(com.stolink.backend.domain.payment.exception.TossPaymentException.class)
        public ResponseEntity<ApiResponse<Void>> handleTossPaymentException(
                        com.stolink.backend.domain.payment.exception.TossPaymentException ex) {
                log.error("Toss payment error: code={}, message={}", ex.getErrorCode(), ex.getMessage());

                HttpStatus status = HttpStatus.resolve(ex.getStatusCode());
                if (status == null) {
                        status = HttpStatus.BAD_GATEWAY;
                }

                return ResponseEntity
                                .status(status)
                                .body(ApiResponse.<Void>builder()
                                                .status(status)
                                                .message(ex.getMessage())
                                                .build());
        }

        @ExceptionHandler(com.stolink.backend.domain.settlement.exception.SettlementExceptions.SettlementNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleSettlementNotFound(
                        com.stolink.backend.domain.settlement.exception.SettlementExceptions.SettlementNotFoundException ex) {
                log.error("Settlement not found: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.NOT_FOUND)
                                                .message(ex.getMessage())
                                                .build());
        }

        @ExceptionHandler(com.stolink.backend.domain.settlement.exception.SettlementExceptions.DuplicateSettlementException.class)
        public ResponseEntity<ApiResponse<Void>> handleDuplicateSettlement(
                        com.stolink.backend.domain.settlement.exception.SettlementExceptions.DuplicateSettlementException ex) {
                log.error("Duplicate settlement: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.CONFLICT)
                                                .message(ex.getMessage())
                                                .build());
        }

        @ExceptionHandler(com.stolink.backend.domain.settlement.exception.SettlementExceptions.InvalidSettlementStatusException.class)
        public ResponseEntity<ApiResponse<Void>> handleInvalidSettlementStatus(
                        com.stolink.backend.domain.settlement.exception.SettlementExceptions.InvalidSettlementStatusException ex) {
                log.error("Invalid settlement status: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.BAD_REQUEST)
                                                .message(ex.getMessage())
                                                .build());
        }

        @ExceptionHandler(com.stolink.backend.domain.settlement.exception.SettlementExceptions.InsufficientRevenueException.class)
        public ResponseEntity<ApiResponse<Void>> handleInsufficientRevenue(
                        com.stolink.backend.domain.settlement.exception.SettlementExceptions.InsufficientRevenueException ex) {
                log.error("Insufficient revenue: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                                                .message(ex.getMessage())
                                                .build());
        }

        @ExceptionHandler(com.stolink.backend.domain.settlement.exception.SettlementExceptions.DuplicateRevenueException.class)
        public ResponseEntity<ApiResponse<Void>> handleDuplicateRevenue(
                        com.stolink.backend.domain.settlement.exception.SettlementExceptions.DuplicateRevenueException ex) {
                log.error("Duplicate revenue: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.CONFLICT)
                                                .message(ex.getMessage())
                                                .build());
        }

        @ExceptionHandler({ SQLGrammarException.class, BadSqlGrammarException.class })
        public ResponseEntity<ApiResponse<Void>> handleSqlGrammarException(Exception ex) {
                log.error("SQL Grammar Exception: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                .message("데이터베이스 처리 중 오류가 발생했습니다.")
                                                .build());
        }
}
