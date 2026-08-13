package com.finance.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyComparison {
    private MonthData month1;
    private MonthData month2;
    private BigDecimal incomeChange;
    private BigDecimal expenseChange;
    private Double incomeChangePercentage;
    private Double expenseChangePercentage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthData {
        private Integer month;
        private Integer year;
        private BigDecimal totalIncome;
        private BigDecimal totalExpenses;
        private BigDecimal balance;
    }
}