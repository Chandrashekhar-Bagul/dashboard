package com.finance.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetStatusDetail {
    private Long budgetId;
    private CategoryResponse category;
    private BigDecimal limitAmount;
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private Double percentageUsed;
    private Integer month;
    private Integer year;
    private Boolean alertEnabled;
    private Integer alertThreshold;
    private Boolean isOverBudget;
    private List<RecentTransaction> recentTransactions;
    private SpendingTrend spendingTrend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentTransaction {
        private Long id;
        private String description;
        private BigDecimal amount;
        private String date;
        private String merchant;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpendingTrend {
        private BigDecimal weeklyAverage;
        private BigDecimal projectedMonthlySpending;
        private String trend; // INCREASING, DECREASING, STABLE
    }
}