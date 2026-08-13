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
public class TransactionStats {
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netSavings;
    private Integer totalTransactions;
    private Integer incomeTransactions;
    private Integer expenseTransactions;
    private BigDecimal averageTransaction;
    private BigDecimal largestExpense;
    private BigDecimal largestIncome;
}