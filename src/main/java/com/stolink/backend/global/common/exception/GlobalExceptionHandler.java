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

import org.springframework.web.bind.MissingRequestHeaderException;

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

        @ExceptionHandler(com.stolink.backend.domain.payment.exception.TossPaymentException.class)
        public ResponseEntity<ApiResponse<Void>> handleTossPaymentException(
                        com.stolink.backend.domain.payment.exception.TossPaymentException ex) {
                log.error("Toss payment error: code={}, message={}", ex.getErrorCode(), ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.BAD_GATEWAY)
                                .body(ApiResponse.<Void>builder()
                                                .status(HttpStatus.BAD_GATEWAY)
                                                .message(ex.getMessage())
                                                .build());
        }
}
