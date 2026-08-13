package com.finance.dashboard.service;

import com.finance.dashboard.dto.*;
import com.finance.dashboard.model.Budget;
import com.finance.dashboard.model.Category;
import com.finance.dashboard.model.User;
import com.finance.dashboard.repository.BudgetRepository;
import com.finance.dashboard.repository.CategoryRepository;
import com.finance.dashboard.repository.TransactionRepository;
import com.finance.dashboard.repository.UserRepository;
import com.finance.dashboard.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional
    @CacheEvict(value = {"dashboardStats", "monthlyReport"}, allEntries = true)
    public BudgetResponse createBudget(BudgetRequest request, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Check if budget already exists
        budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(
                        user.getId(), request.getCategoryId(), request.getMonth(), request.getYear())
                .ifPresent(b -> {
                    throw new RuntimeException("Budget already exists for this category and month");
                });

        Budget budget = Budget.builder()
                .user(user)
                .category(category)
                .limitAmount(request.getLimitAmount())
                .month(request.getMonth())
                .year(request.getYear())
                .alertEnabled(request.getAlertEnabled())
                .alertThreshold(request.getAlertThreshold())
                .build();

        budget = budgetRepository.save(budget);
        return mapToResponse(budget, user.getId());
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getAllUserBudgets(Long userId) {
        List<Budget> budgets = budgetRepository.findByUserId(userId);
        return budgets.stream()
                .map(b -> mapToResponse(b, userId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudgetById(Long id, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        if (!budget.getUser().getId().equals(principal.getId())) {
            throw new RuntimeException("Unauthorized access");
        }

        return mapToResponse(budget, principal.getId());
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetsByMonthYear(Long userId, Integer month, Integer year) {
        List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYear(userId, month, year);
        return budgets.stream()
                .map(b -> mapToResponse(b, userId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getCurrentMonthBudgets(Long userId) {
        LocalDate now = LocalDate.now();
        return getBudgetsByMonthYear(userId, now.getMonthValue(), now.getYear());
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetsByCategory(Long userId, Long categoryId) {
        List<Budget> budgets = budgetRepository.findByUserIdAndCategoryId(userId, categoryId);
        return budgets.stream()
                .map(b -> mapToResponse(b, userId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetsByYear(Long userId, Integer year) {
        List<Budget> budgets = budgetRepository.findByUserIdAndYear(userId, year);
        return budgets.stream()
                .map(b -> mapToResponse(b, userId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BudgetStatusDetail getBudgetStatusDetail(Long id, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        if (!budget.getUser().getId().equals(principal.getId())) {
            throw new RuntimeException("Unauthorized access");
        }

        BudgetResponse budgetResponse = mapToResponse(budget, principal.getId());

        // Get recent transactions for this category
        List<BudgetStatusDetail.RecentTransaction> recentTransactions =
                transactionRepository.findTop5ByUserIdAndCategoryIdOrderByTransactionDateDesc(
                                principal.getId(), budget.getCategory().getId())
                        .stream()
                        .map(t -> BudgetStatusDetail.RecentTransaction.builder()
                                .id(t.getId())
                                .description(t.getDescription())
                                .amount(t.getAmount())
                                .date(t.getTransactionDate().toString())
                                .merchant(t.getMerchant())
                                .build())
                        .collect(Collectors.toList());

        return BudgetStatusDetail.builder()
                .budgetId(budgetResponse.getId())
                .category(budgetResponse.getCategory())
                .limitAmount(budgetResponse.getLimitAmount())
                .spentAmount(budgetResponse.getSpentAmount())
                .remainingAmount(budgetResponse.getRemainingAmount())
                .percentageUsed(budgetResponse.getPercentageUsed())
                .month(budgetResponse.getMonth())
                .year(budgetResponse.getYear())
                .alertEnabled(budgetResponse.getAlertEnabled())
                .alertThreshold(budgetResponse.getAlertThreshold())
                .isOverBudget(budgetResponse.getIsOverBudget())
                .recentTransactions(recentTransactions)
                .build();
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getOverBudgetAlerts(Long userId) {
        LocalDate now = LocalDate.now();
        List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYear(
                userId, now.getMonthValue(), now.getYear());

        return budgets.stream()
                .map(b -> mapToResponse(b, userId))
                .filter(BudgetResponse::getIsOverBudget)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = {"dashboardStats", "monthlyReport"}, allEntries = true)
    public BudgetResponse updateBudget(Long id, BudgetRequest request, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        if (!budget.getUser().getId().equals(principal.getId())) {
            throw new RuntimeException("Unauthorized access");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        budget.setCategory(category);
        budget.setLimitAmount(request.getLimitAmount());
        budget.setMonth(request.getMonth());
        budget.setYear(request.getYear());
        budget.setAlertEnabled(request.getAlertEnabled());
        budget.setAlertThreshold(request.getAlertThreshold());

        budget = budgetRepository.save(budget);
        return mapToResponse(budget, principal.getId());
    }

    @Transactional
    @CacheEvict(value = {"dashboardStats", "monthlyReport"}, allEntries = true)
    public BudgetResponse updateAlertSettings(Long id, BudgetAlertSettingsRequest request,
                                              Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        if (!budget.getUser().getId().equals(principal.getId())) {
            throw new RuntimeException("Unauthorized access");
        }

        if (request.getAlertEnabled() != null) {
            budget.setAlertEnabled(request.getAlertEnabled());
        }
        if (request.getAlertThreshold() != null) {
            budget.setAlertThreshold(request.getAlertThreshold());
        }

        budget = budgetRepository.save(budget);
        return mapToResponse(budget, principal.getId());
    }

    @Transactional
    @CacheEvict(value = {"dashboardStats", "monthlyReport"}, allEntries = true)
    public void deleteBudget(Long id, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        if (!budget.getUser().getId().equals(principal.getId())) {
            throw new RuntimeException("Unauthorized access");
        }

        budgetRepository.delete(budget);
    }

    @Transactional
    public BudgetResponse cloneBudget(Long id, Integer targetMonth, Integer targetYear,
                                      Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Budget sourceBudget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        if (!sourceBudget.getUser().getId().equals(principal.getId())) {
            throw new RuntimeException("Unauthorized access");
        }

        // Check if budget already exists for target month
        budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(
                        principal.getId(), sourceBudget.getCategory().getId(), targetMonth, targetYear)
                .ifPresent(b -> {
                    throw new RuntimeException("Budget already exists for target month");
                });

        Budget newBudget = Budget.builder()
                .user(sourceBudget.getUser())
                .category(sourceBudget.getCategory())
                .limitAmount(sourceBudget.getLimitAmount())
                .month(targetMonth)
                .year(targetYear)
                .alertEnabled(sourceBudget.getAlertEnabled())
                .alertThreshold(sourceBudget.getAlertThreshold())
                .build();

        newBudget = budgetRepository.save(newBudget);
        return mapToResponse(newBudget, principal.getId());
    }

    @Transactional
    public List<BudgetResponse> copyBudgets(Long userId, Integer sourceMonth, Integer sourceYear,
                                            Integer targetMonth, Integer targetYear) {
        List<Budget> sourceBudgets = budgetRepository.findByUserIdAndMonthAndYear(
                userId, sourceMonth, sourceYear);

        if (sourceBudgets.isEmpty()) {
            throw new RuntimeException("No budgets found for source month");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Budget> newBudgets = new ArrayList<>();

        for (Budget source : sourceBudgets) {
            // Skip if budget already exists
            if (budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(
                    userId, source.getCategory().getId(), targetMonth, targetYear).isEmpty()) {

                Budget newBudget = Budget.builder()
                        .user(user)
                        .category(source.getCategory())
                        .limitAmount(source.getLimitAmount())
                        .month(targetMonth)
                        .year(targetYear)
                        .alertEnabled(source.getAlertEnabled())
                        .alertThreshold(source.getAlertThreshold())
                        .build();

                newBudgets.add(budgetRepository.save(newBudget));
            }
        }

        return newBudgets.stream()
                .map(b -> mapToResponse(b, userId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BudgetSummary getBudgetSummary(Long userId, Integer month, Integer year) {
        if (month == null || year == null) {
            LocalDate now = LocalDate.now();
            month = now.getMonthValue();
            year = now.getYear();
        }

        List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYear(userId, month, year);

        BigDecimal totalBudgeted = BigDecimal.ZERO;
        BigDecimal totalSpent = BigDecimal.ZERO;
        int overBudgetCount = 0;
        int underBudgetCount = 0;

        for (Budget budget : budgets) {
            totalBudgeted = totalBudgeted.add(budget.getLimitAmount());

            BigDecimal spent = transactionRepository.sumAmountByCategoryAndMonthYear(
                    userId, budget.getCategory().getId(), year, month);
            totalSpent = totalSpent.add(spent);

            if (spent.compareTo(budget.getLimitAmount()) > 0) {
                overBudgetCount++;
            } else {
                underBudgetCount++;
            }
        }

        Double percentageUsed = totalBudgeted.compareTo(BigDecimal.ZERO) > 0
                ? totalSpent.divide(totalBudgeted, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        return BudgetSummary.builder()
                .totalBudgeted(totalBudgeted)
                .totalSpent(totalSpent)
                .totalRemaining(totalBudgeted.subtract(totalSpent))
                .overallPercentageUsed(percentageUsed)
                .totalBudgets(budgets.size())
                .overBudgetCount(overBudgetCount)
                .underBudgetCount(underBudgetCount)
                .build();
    }

    @Transactional(readOnly = true)
    public List<BudgetTrend> getBudgetTrends(Long userId, Integer months) {
        List<BudgetTrend> trends = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (int i = 0; i < months; i++) {
            YearMonth yearMonth = YearMonth.now().minusMonths(i);
            int month = yearMonth.getMonthValue();
            int year = yearMonth.getYear();

            BudgetSummary summary = getBudgetSummary(userId, month, year);

            trends.add(BudgetTrend.builder()
                    .month(month)
                    .year(year)
                    .monthYear(yearMonth.format(DateTimeFormatter.ofPattern("MMM yyyy")))
                    .budgeted(summary.getTotalBudgeted())
                    .spent(summary.getTotalSpent())
                    .remaining(summary.getTotalRemaining())
                    .percentageUsed(summary.getOverallPercentageUsed())
                    .overBudget(summary.getOverBudgetCount() > 0)
                    .build());
        }

        return trends;
    }

    @Transactional(readOnly = true)
    public BudgetPerformance getBudgetPerformance(Long userId, Integer month, Integer year) {
        List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYear(userId, month, year);

        BigDecimal totalBudgeted = BigDecimal.ZERO;
        BigDecimal totalSpent = BigDecimal.ZERO;
        int onTrack = 0;
        int overBudget = 0;

        List<BudgetPerformance.CategoryPerformance> performances = new ArrayList<>();

        for (Budget budget : budgets) {
            totalBudgeted = totalBudgeted.add(budget.getLimitAmount());

            BigDecimal spent = transactionRepository.sumAmountByCategoryAndMonthYear(
                    userId, budget.getCategory().getId(), year, month);
            totalSpent = totalSpent.add(spent);

            Double performance = budget.getLimitAmount().compareTo(BigDecimal.ZERO) > 0
                    ? spent.divide(budget.getLimitAmount(), 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue()
                    : 0.0;

            String status;
            if (performance >= 100) {
                status = "OVER_BUDGET";
                overBudget++;
            } else if (performance >= budget.getAlertThreshold()) {
                status = "WARNING";
            } else {
                status = "ON_TRACK";
                onTrack++;
            }

            performances.add(BudgetPerformance.CategoryPerformance.builder()
                    .categoryName(budget.getCategory().getName())
                    .categoryIcon(budget.getCategory().getIcon())
                    .categoryColor(budget.getCategory().getColor())
                    .budgeted(budget.getLimitAmount())
                    .spent(spent)
                    .performance(performance)
                    .status(status)
                    .build());
        }

        Double overallPerformance = totalBudgeted.compareTo(BigDecimal.ZERO) > 0
                ? totalSpent.divide(totalBudgeted, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        return BudgetPerformance.builder()
                .month(month)
                .year(year)
                .totalBudgeted(totalBudgeted)
                .totalSpent(totalSpent)
                .overallPerformance(overallPerformance)
                .categoriesOnTrack(onTrack)
                .categoriesOverBudget(overBudget)
                .categoryPerformances(performances)
                .build();
    }

    @Transactional
    public BudgetResponse adjustBudgetAmount(Long id, BigDecimal newAmount, String reason,
                                             Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        if (!budget.getUser().getId().equals(principal.getId())) {
            throw new RuntimeException("Unauthorized access");
        }

        budget.setLimitAmount(newAmount);
        budget = budgetRepository.save(budget);

        return mapToResponse(budget, principal.getId());
    }

    @Transactional
    public Boolean checkBudgetAlert(Long userId, Long categoryId, int year, int month) {
        Budget budget = budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(
                userId, categoryId, month, year).orElse(null);

        if (budget == null || !budget.getAlertEnabled()) {
            return false;
        }

        BigDecimal spent = transactionRepository.sumAmountByCategoryAndMonthYear(
                userId, categoryId, year, month);
        BigDecimal limit = budget.getLimitAmount();

        if (limit.compareTo(BigDecimal.ZERO) > 0) {
            Double percentage = spent.divide(limit, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();

            return percentage >= budget.getAlertThreshold();
        }
        return false;
    }

    private BudgetResponse mapToResponse(Budget budget, Long userId) {
        BigDecimal spent = transactionRepository.sumAmountByCategoryAndMonthYear(
                userId, budget.getCategory().getId(), budget.getYear(), budget.getMonth());

        BigDecimal remaining = budget.getLimitAmount().subtract(spent);

        Double percentageUsed = budget.getLimitAmount().compareTo(BigDecimal.ZERO) > 0
                ? spent.divide(budget.getLimitAmount(), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        return BudgetResponse.builder()
                .id(budget.getId())
                .category(CategoryResponse.builder()
                        .id(budget.getCategory().getId())
                        .name(budget.getCategory().getName())
                        .description(budget.getCategory().getDescription())
                        .icon(budget.getCategory().getIcon())
                        .color(budget.getCategory().getColor())
                        .type(budget.getCategory().getType())
                        .build())
                .limitAmount(budget.getLimitAmount())
                .spentAmount(spent)
                .remainingAmount(remaining)
                .percentageUsed(percentageUsed)
                .month(budget.getMonth())
                .year(budget.getYear())
                .alertEnabled(budget.getAlertEnabled())
                .alertThreshold(budget.getAlertThreshold())
                .isOverBudget(percentageUsed >= budget.getAlertThreshold())
                .build();
    }
}