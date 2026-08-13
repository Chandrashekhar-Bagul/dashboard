package com.finance.dashboard.service;

import com.finance.dashboard.dto.AIInsightResponse;
import com.finance.dashboard.model.AIInsight;
import com.finance.dashboard.model.AIInsight.InsightType;
import com.finance.dashboard.model.Budget;
import com.finance.dashboard.model.Transaction;
import com.finance.dashboard.model.User;
import com.finance.dashboard.repository.AIInsightRepository;
import com.finance.dashboard.repository.BudgetRepository;
import com.finance.dashboard.repository.TransactionRepository;
import com.finance.dashboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AIInsightService {

    private final AIInsightRepository aiInsightRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;

    @Async
    public void generateInsightsAsync(Long userId) {
        generateSpendingInsight(userId);
        generateSavingTip(userId);
        generateBudgetReview(userId);
    }

    @Transactional
    public int generateAllInsights(Long userId) {
        int count = 0;

        generateSpendingInsight(userId);
        count++;

        generateSavingTip(userId);
        count++;

        generateBudgetReview(userId);
        count++;

        return count;
    }

    private void generateSpendingInsight(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        YearMonth currentMonth = YearMonth.now();
        YearMonth lastMonth = currentMonth.minusMonths(1);

        BigDecimal currentSpent = transactionRepository.sumByUserAndTypeAndDateRange(
                userId, Transaction.TransactionType.EXPENSE,
                currentMonth.atDay(1), currentMonth.atEndOfMonth());

        BigDecimal lastSpent = transactionRepository.sumByUserAndTypeAndDateRange(
                userId, Transaction.TransactionType.EXPENSE,
                lastMonth.atDay(1), lastMonth.atEndOfMonth());

        if (currentSpent.compareTo(BigDecimal.ZERO) > 0 && lastSpent.compareTo(BigDecimal.ZERO) > 0) {
            double changePercent = currentSpent.subtract(lastSpent)
                    .divide(lastSpent, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();

            String insightMsg;
            if (changePercent > 0) {
                insightMsg = String.format("Your spending increased by %.1f%% this month compared to last month. 📈",
                        Math.abs(changePercent));
            } else {
                insightMsg = String.format("Great job! Your spending decreased by %.1f%% this month. 🎉",
                        Math.abs(changePercent));
            }

            saveInsight(userId, insightMsg, InsightType.SPENDING_ALERT);
        }
    }

    private void generateSavingTip(Long userId) {
        String tip = "💡 Pro Tip: Set up an automatic transfer of 10% of your income to savings right after payday!";
        saveInsight(userId, tip, InsightType.SAVING_TIP);
    }

    private void generateBudgetReview(Long userId) {
        LocalDate now = LocalDate.now();
        List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYear(
                userId, now.getMonthValue(), now.getYear());

        if (budgets.isEmpty()) return;

        int overBudget = 0;
        int onTrack = 0;

        for (Budget budget : budgets) {
            BigDecimal spent = transactionRepository.sumAmountByCategoryAndMonthYear(
                    userId, budget.getCategory().getId(), now.getYear(), now.getMonthValue());

            if (spent.compareTo(budget.getLimitAmount()) > 0) {
                overBudget++;
            } else {
                onTrack++;
            }
        }

        String message = String.format("Budget Review: %d categories on track, %d over budget. %s",
                onTrack, overBudget, overBudget > 0 ? "⚠️" : "✅");

        saveInsight(userId, message, InsightType.BUDGET_WARNING);
    }

    @Transactional
    protected void saveInsight(Long userId, String message, InsightType type) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            AIInsight insight = AIInsight.builder()
                    .user(user)
                    .insight(message)
                    .type(type)
                    .isRead(false)
                    .build();
            aiInsightRepository.save(insight);
        }
    }

    @Transactional(readOnly = true)
    public List<AIInsightResponse> getUserInsights(Long userId, Boolean isRead) {
        List<AIInsight> insights;

        if (isRead != null) {
            insights = aiInsightRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, isRead);
        } else {
            insights = aiInsightRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }

        return insights.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AIInsightResponse> getInsightsByType(Long userId, String type) {
        InsightType insightType = InsightType.valueOf(type.toUpperCase());
        return aiInsightRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, insightType)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markInsightAsRead(Long insightId, Long userId) {
        AIInsight insight = aiInsightRepository.findById(insightId)
                .orElseThrow(() -> new RuntimeException("Insight not found"));

        if (!insight.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        insight.setIsRead(true);
        aiInsightRepository.save(insight);
    }

    @Transactional
    public int markAllInsightsAsRead(Long userId) {
        List<AIInsight> insights = aiInsightRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false);

        insights.forEach(insight -> insight.setIsRead(true));
        aiInsightRepository.saveAll(insights);

        return insights.size();
    }

    @Transactional
    public void deleteInsight(Long insightId, Long userId) {
        AIInsight insight = aiInsightRepository.findById(insightId)
                .orElseThrow(() -> new RuntimeException("Insight not found"));

        if (!insight.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        aiInsightRepository.delete(insight);
    }

    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        return aiInsightRepository.countByUserIdAndIsRead(userId, false);
    }

    private AIInsightResponse mapToResponse(AIInsight insight) {
        return AIInsightResponse.builder()
                .id(insight.getId())
                .insight(insight.getInsight())
                .type(insight.getType())
                .isRead(insight.getIsRead())
                .createdAt(insight.getCreatedAt())
                .build();
    }
}