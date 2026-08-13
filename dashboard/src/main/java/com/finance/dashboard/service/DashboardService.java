package com.finance.dashboard.service;

import com.finance.dashboard.dto.DashboardStats;
import com.finance.dashboard.model.Transaction.TransactionType;
import com.finance.dashboard.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final BudgetService budgetService;

    @Cacheable(value = "dashboardStats", key = "#userId + '_' + #month + '_' + #year")
    public DashboardStats getDashboardStats(Long userId, int month, int year) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        BigDecimal income = transactionRepository.sumByUserAndTypeAndDateRange(userId, TransactionType.INCOME, start, end);
        BigDecimal expenses = transactionRepository.sumByUserAndTypeAndDateRange(userId, TransactionType.EXPENSE, start, end);
        BigDecimal balance = income.subtract(expenses);
        Long totalTransactions = transactionRepository.countByUserId(userId);

        double savingsRate = income.compareTo(BigDecimal.ZERO) > 0
                ? income.subtract(expenses).multiply(BigDecimal.valueOf(100)).divide(income, 2, RoundingMode.HALF_UP).doubleValue()
                : 0;

        return DashboardStats.builder()
                .totalIncome(income)
                .totalExpenses(expenses)
                .balance(balance)
                .totalTransactions(totalTransactions.intValue())
                .savingsRate(BigDecimal.valueOf(savingsRate))
                .budgetStatus(budgetService.getCurrentMonthBudgets(userId))
                .categoryBreakdown(calculateCategoryBreakdown(userId, month, year))
                .monthlyTrend(calculateMonthlyTrend(userId, year))
                .build();
    }

    private List<DashboardStats.CategorySpending> calculateCategoryBreakdown(Long userId, int month, int year) {
        // Simplified logic for brevity - in prod, use JPQL query grouping by category
        return new ArrayList<>();
    }

    private Map<String, BigDecimal> calculateMonthlyTrend(Long userId, int year) {
        // Simplified logic for brevity
        return new HashMap<>();
    }
}