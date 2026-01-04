package com.stolink.backend.domain.payment.controller;

import com.stolink.backend.domain.payment.dto.request.CreditUseRequest;
import com.stolink.backend.domain.payment.dto.response.CreditCheckResponse;
import com.stolink.backend.domain.payment.dto.response.CreditResponse;
import com.stolink.backend.domain.payment.dto.response.CreditTransactionResponse;
import com.stolink.backend.domain.payment.entity.CreditTransactionType;
import com.stolink.backend.domain.payment.service.CreditService;
import com.stolink.backend.global.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/credits")
@RequiredArgsConstructor
public class CreditController {

    private final CreditService creditService;

    /**
     * 크레딧 잔액 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CreditResponse>> getCredit(
            @RequestHeader("X-User-Id") UUID userId) {
        CreditResponse response = creditService.getCredit(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 크레딧 사용
     */
    @PostMapping("/use")
    public ResponseEntity<ApiResponse<CreditResponse>> useCredit(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreditUseRequest request) {
        CreditResponse response = creditService.useCredit(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 크레딧 사용 가능 여부 확인 (상세 정보 포함)
     */
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<CreditCheckResponse>> checkCredit(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam Long amount) {
        CreditCheckResponse response = creditService.checkCredit(userId, amount);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 크레딧 거래 내역 조회
     */
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Page<CreditTransactionResponse>>> getTransactions(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(required = false) CreditTransactionType type,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<CreditTransactionResponse> response = type != null
            ? creditService.getTransactionsByType(userId, type, pageable)
            : creditService.getTransactions(userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
