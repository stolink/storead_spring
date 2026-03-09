package com.stolink.backend.domain.settlement.dto;

import java.util.List;
import java.util.UUID;

public record DashboardResponse(
        Long totalEarned,
        Long pendingBalance,
        Long totalSettled,
        List<WorkRevenueItem> revenueByWork,
        List<MonthlyRevenueItem> revenueByMonth,
        List<RevenueTransactionResponse> recentTransactions
) {
    public record WorkRevenueItem(
            UUID workId,
            String workTitle,
            Long totalRevenue,
            Long transactionCount
    ) {}

    public record MonthlyRevenueItem(
            String month,
            Long totalRevenue,
            Long transactionCount
    ) {}

    public static DashboardResponse empty() {
        return new DashboardResponse(0L, 0L, 0L, List.of(), List.of(), List.of());
    }
}
