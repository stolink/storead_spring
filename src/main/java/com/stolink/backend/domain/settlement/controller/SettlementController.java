package com.stolink.backend.domain.settlement.controller;

import com.stolink.backend.domain.settlement.dto.DashboardResponse;
import com.stolink.backend.domain.settlement.dto.RevenueTransactionResponse;
import com.stolink.backend.domain.settlement.dto.SettlementRejectRequest;
import com.stolink.backend.domain.settlement.dto.SettlementResponse;
import com.stolink.backend.domain.settlement.service.SettlementService;
import com.stolink.backend.global.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(settlementService.getDashboard(userId)));
    }

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<Page<RevenueTransactionResponse>>> getRevenueTransactions(
            @AuthenticationPrincipal UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                settlementService.getRevenueTransactions(userId, pageable)));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<Page<SettlementResponse>>> getSettlements(
            @AuthenticationPrincipal UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                settlementService.getSettlements(userId, pageable)));
    }

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<SettlementResponse>> requestSettlement(
            @AuthenticationPrincipal UUID userId,
            @RequestParam LocalDate periodStart,
            @RequestParam LocalDate periodEnd) {
        SettlementResponse response = settlementService.createSettlement(userId, periodStart, periodEnd);
        return ResponseEntity.ok(ApiResponse.created(response));
    }

    @PostMapping("/{settlementId}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmSettlement(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID settlementId) {
        settlementService.confirmSettlement(userId, settlementId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/{settlementId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectSettlement(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID settlementId,
            @Valid @RequestBody SettlementRejectRequest request) {
        settlementService.rejectSettlement(userId, settlementId, request.reason());
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
