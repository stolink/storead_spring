package com.stolink.backend.domain.payment.controller;

import com.stolink.backend.domain.payment.dto.request.PaymentCancelRequest;
import com.stolink.backend.domain.payment.dto.request.PaymentConfirmRequest;
import com.stolink.backend.domain.payment.dto.request.PaymentPrepareRequest;
import com.stolink.backend.domain.payment.dto.response.CreditPackageResponse;
import com.stolink.backend.domain.payment.dto.response.PaymentPrepareResponse;
import com.stolink.backend.domain.payment.dto.response.PaymentResponse;
import com.stolink.backend.domain.payment.service.PaymentService;
import com.stolink.backend.global.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({ "/api/v1/payments", "/api/payments" })
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 결제 준비 (주문 생성)
     */
    @PostMapping("/prepare")
    public ResponseEntity<ApiResponse<PaymentPrepareResponse>> preparePayment(
            @AuthenticationPrincipal UUID userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PaymentPrepareRequest request) {
        PaymentPrepareResponse response = paymentService.preparePayment(userId, request, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 결제 승인
     */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody PaymentConfirmRequest request) {
        PaymentResponse response = paymentService.confirmPayment(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 결제 취소
     */
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancelPayment(
            @AuthenticationPrincipal UUID userId,
            @PathVariable String paymentId,
            @Valid @RequestBody PaymentCancelRequest request) {
        PaymentResponse response = paymentService.cancelPayment(userId, paymentId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 결제 내역 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getPayments(
            @AuthenticationPrincipal UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PaymentResponse> response = paymentService.getPayments(userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 결제 상세 조회
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @AuthenticationPrincipal UUID userId,
            @PathVariable String paymentId) {
        PaymentResponse response = paymentService.getPayment(userId, paymentId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 크레딧 패키지 목록 조회
     */
    @GetMapping("/packages")
    public ResponseEntity<ApiResponse<List<CreditPackageResponse>>> getCreditPackages() {
        List<CreditPackageResponse> response = paymentService.getCreditPackages();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
