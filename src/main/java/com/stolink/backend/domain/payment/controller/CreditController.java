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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.stolink.backend.domain.chapter.service.ChapterPurchaseService;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@RestController
@RequestMapping({ "/api/v1/credits", "/api/credits" })
@RequiredArgsConstructor
@Slf4j
public class CreditController {

    private final CreditService creditService;
    private final ChapterPurchaseService chapterPurchaseService;

    /**
     * 크레딧 잔액 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CreditResponse>> getCredit(
            @AuthenticationPrincipal UUID userId) {
        CreditResponse response = creditService.getCredit(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 크레딧 사용
     */
    @PostMapping("/use")
    public ResponseEntity<ApiResponse<CreditResponse>> useCredit(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreditUseRequest request) {

        // 챕터 구매인 경우 ChapterPurchaseService를 통해 처리하여 구매 기록 생성 보장
        if ("CHAPTER".equalsIgnoreCase(request.referenceType()) && request.referenceId() != null) {
            try {
                UUID chapterId = UUID.fromString(request.referenceId());
                chapterPurchaseService.purchaseChapter(userId, chapterId);
                return ResponseEntity.ok(ApiResponse.ok(creditService.getCredit(userId)));
            } catch (Exception e) {
                log.error("Failed to process chapter purchase via credit use endpoint: userId={}, chapterId={}",
                        userId, request.referenceId(), e);
                // 에러 발생 시 기존 방식으로 진행하지 않고 에러 응답 (구매 기록 없이 크레딧만 차감되는 것 방지)
                throw e;
            }
        }

        CreditResponse response = creditService.useCredit(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 크레딧 사용 가능 여부 확인 (상세 정보 포함)
     */
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<CreditCheckResponse>> checkCredit(
            @AuthenticationPrincipal UUID userId,
            @RequestParam Long amount) {
        CreditCheckResponse response = creditService.checkCredit(userId, amount);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 크레딧 거래 내역 조회
     */
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Page<CreditTransactionResponse>>> getTransactions(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(required = false) CreditTransactionType type,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<CreditTransactionResponse> response = type != null
                ? creditService.getTransactionsByType(userId, type, pageable)
                : creditService.getTransactions(userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
