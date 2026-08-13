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
public class BudgetTrend {
    private Integer month;
    private Integer year;
    private String monthYear; // e.g., "Jan 2024"
    private BigDecimal budgeted;
    private BigDecimal spent;
    private BigDecimal remaining;
    private Double percentageUsed;
    private Boolean overBudget;
}