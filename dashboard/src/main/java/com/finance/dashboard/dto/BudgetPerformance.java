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
public class BudgetPerformance {
    private Integer month;
    private Integer year;
    private BigDecimal totalBudgeted;
    private BigDecimal totalSpent;
    private Double overallPerformance; // percentage
    private Integer categoriesOnTrack;
    private Integer categoriesOverBudget;
    private List<CategoryPerformance> categoryPerformances;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryPerformance {
        private String categoryName;
        private String categoryIcon;
        private String categoryColor;
        private BigDecimal budgeted;
        private BigDecimal spent;
        private Double performance;
        private String status; // ON_TRACK, WARNING, OVER_BUDGET
    }
}