package com.finance.dashboard.service;

import com.finance.dashboard.dto.CategoryResponse;
import com.finance.dashboard.dto.MonthlyComparison;
import com.finance.dashboard.dto.TransactionRequest;
import com.finance.dashboard.dto.TransactionResponse;
import com.finance.dashboard.dto.TransactionStats;
import com.finance.dashboard.model.Category;
import com.finance.dashboard.model.Transaction;
import com.finance.dashboard.model.Transaction.TransactionType;
import com.finance.dashboard.model.User;
import com.finance.dashboard.repository.CategoryRepository;
import com.finance.dashboard.repository.TransactionRepository;
import com.finance.dashboard.repository.UserRepository;
import com.finance.dashboard.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final BudgetService budgetService;
    private final AIInsightService aiInsightService;

    // ==================== CRUD ====================

    @Transactional
    @CacheEvict(value = {"dashboardStats", "monthlyReport", "transactions"}, allEntries = true)
    public TransactionResponse createTransaction(TransactionRequest request, Authentication authentication) {
        Long userId = getUserId(authentication);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Transaction transaction = Transaction.builder()
                .user(user)
                .category(category)
                .description(request.getDescription())
                .amount(request.getAmount())
                .type(request.getType())
                .transactionDate(request.getTransactionDate())
                .merchant(request.getMerchant())
                .notes(request.getNotes())
                .isRecurring(request.getIsRecurring() != null && request.getIsRecurring())
                .build();

        transaction = transactionRepository.save(transaction);

        // Budget check + insights (non-fatal if they fail)
        try {
            budgetService.checkBudgetAlert(userId, category.getId(),
                    transaction.getTransactionDate().getYear(),
                    transaction.getTransactionDate().getMonthValue());
            aiInsightService.generateInsightsAsync(userId);
        } catch (Exception ex) {
            log.warn("Post-transaction processing failed: {}", ex.getMessage());
        }

        return mapToResponse(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Long id, Authentication authentication) {
        Long userId = getUserId(authentication);

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!transaction.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        return mapToResponse(transaction);
    }

    @Transactional
    @CacheEvict(value = {"dashboardStats", "monthlyReport", "transactions"}, allEntries = true)
    public TransactionResponse updateTransaction(Long id, TransactionRequest request,
                                                 Authentication authentication) {
        Long userId = getUserId(authentication);

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!transaction.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        transaction.setCategory(category);
        transaction.setDescription(request.getDescription());
        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setMerchant(request.getMerchant());
        transaction.setNotes(request.getNotes());
        transaction.setIsRecurring(request.getIsRecurring() != null && request.getIsRecurring());

        transaction = transactionRepository.save(transaction);
        return mapToResponse(transaction);
    }

    @Transactional
    @CacheEvict(value = {"dashboardStats", "monthlyReport", "transactions"}, allEntries = true)
    public void deleteTransaction(Long id, Authentication authentication) {
        Long userId = getUserId(authentication);

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!transaction.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        transactionRepository.delete(transaction);
    }

    @Transactional
    @CacheEvict(value = {"dashboardStats", "monthlyReport", "transactions"}, allEntries = true)
    public int bulkDeleteTransactions(List<Long> transactionIds, Authentication authentication) {
        Long userId = getUserId(authentication);

        List<Transaction> owned = transactionRepository.findAllById(transactionIds).stream()
                .filter(t -> t.getUser().getId().equals(userId))
                .collect(Collectors.toList());

        transactionRepository.deleteAll(owned);
        return owned.size();
    }

    // ==================== QUERIES ====================

    @Transactional(readOnly = true)
    public List<TransactionResponse> getUserTransactions(Long userId) {
        return transactionRepository.findByUserIdOrderByTransactionDateDesc(userId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getUserTransactionsPaginated(Long userId, Pageable pageable) {
        return transactionRepository.findByUserId(userId, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByDateRange(Long userId,
                                                                LocalDate startDate,
                                                                LocalDate endDate) {
        return transactionRepository
                .findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(userId, startDate, endDate)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByCategory(Long userId, Long categoryId,
                                                               LocalDate startDate, LocalDate endDate) {
        List<Transaction> transactions = (startDate != null && endDate != null)
                ? transactionRepository.findByUserIdAndCategoryIdAndTransactionDateBetween(
                userId, categoryId, startDate, endDate)
                : transactionRepository.findByUserIdAndCategoryId(userId, categoryId);

        return transactions.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByType(Long userId, String type,
                                                           Integer month, Integer year) {
        TransactionType txType = TransactionType.valueOf(type.toUpperCase());

        List<Transaction> transactions = (month != null && year != null)
                ? transactionRepository.findByUserAndTypeAndMonthYear(userId, txType, year, month)
                : transactionRepository.findByUserIdAndType(userId, txType);

        return transactions.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> searchTransactions(Long userId, String query) {
        return transactionRepository.searchByKeyword(userId, query)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getRecurringTransactions(Long userId) {
        return transactionRepository.findByUserIdAndIsRecurring(userId, true)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ==================== STATS ====================

    @Transactional(readOnly = true)
    public TransactionStats getTransactionStats(Long userId, Integer month, Integer year) {
        YearMonth ym = (month != null && year != null) ? YearMonth.of(year, month) : YearMonth.now();
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Transaction> transactions = transactionRepository
                .findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(userId, start, end);

        List<Transaction> incomes = transactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME).collect(Collectors.toList());
        List<Transaction> expenses = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE).collect(Collectors.toList());

        BigDecimal totalIncome = sum(incomes);
        BigDecimal totalExpenses = sum(expenses);

        BigDecimal average = transactions.isEmpty()
                ? BigDecimal.ZERO
                : totalIncome.add(totalExpenses)
                .divide(BigDecimal.valueOf(transactions.size()), 2, RoundingMode.HALF_UP);

        return TransactionStats.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netSavings(totalIncome.subtract(totalExpenses))
                .totalTransactions(transactions.size())
                .incomeTransactions(incomes.size())
                .expenseTransactions(expenses.size())
                .averageTransaction(average)
                .largestExpense(max(expenses))
                .largestIncome(max(incomes))
                .build();
    }

    @Transactional(readOnly = true)
    public MonthlyComparison compareMonths(Long userId, Integer month1, Integer year1,
                                           Integer month2, Integer year2) {
        TransactionStats s1 = getTransactionStats(userId, month1, year1);
        TransactionStats s2 = getTransactionStats(userId, month2, year2);

        return MonthlyComparison.builder()
                .month1(MonthlyComparison.MonthData.builder()
                        .month(month1).year(year1)
                        .totalIncome(s1.getTotalIncome())
                        .totalExpenses(s1.getTotalExpenses())
                        .balance(s1.getNetSavings())
                        .build())
                .month2(MonthlyComparison.MonthData.builder()
                        .month(month2).year(year2)
                        .totalIncome(s2.getTotalIncome())
                        .totalExpenses(s2.getTotalExpenses())
                        .balance(s2.getNetSavings())
                        .build())
                .incomeChange(s2.getTotalIncome().subtract(s1.getTotalIncome()))
                .expenseChange(s2.getTotalExpenses().subtract(s1.getTotalExpenses()))
                .incomeChangePercentage(percentChange(s1.getTotalIncome(), s2.getTotalIncome()))
                .expenseChangePercentage(percentChange(s1.getTotalExpenses(), s2.getTotalExpenses()))
                .build();
    }

    // ==================== IMPORT / EXPORT ====================

    @Transactional
    @CacheEvict(value = {"dashboardStats", "monthlyReport", "transactions"}, allEntries = true)
    public Map<String, Object> uploadTransactionsFromCSV(MultipartFile file, Long userId) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        int success = 0;
        int lineNo = 1;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean header = true;

            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (header) { header = false; continue; }
                if (line.isBlank()) continue;

                try {
                    // CSV: date,description,category,amount,type,merchant
                    String[] c = line.split(",", -1);

                    Category category = categoryRepository.findByName(c[2].trim())
                            .orElseThrow(() -> new RuntimeException("Unknown category: " + c[2].trim()));

                    Transaction tx = Transaction.builder()
                            .user(user)
                            .category(category)
                            .transactionDate(LocalDate.parse(c[0].trim()))
                            .description(c[1].trim())
                            .amount(new BigDecimal(c[3].trim()))
                            .type(TransactionType.valueOf(c[4].trim().toUpperCase()))
                            .merchant(c.length > 5 ? c[5].trim() : null)
                            .isRecurring(false)
                            .build();

                    transactionRepository.save(tx);
                    success++;
                } catch (Exception e) {
                    errors.add("Line " + lineNo + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read CSV: " + e.getMessage());
        }

        result.put("successCount", success);
        result.put("errorCount", errors.size());
        result.put("errors", errors);
        return result;
    }

    @Transactional(readOnly = true)
    public byte[] exportTransactionsToCSV(Long userId, LocalDate startDate, LocalDate endDate) {
        List<Transaction> transactions = (startDate != null && endDate != null)
                ? transactionRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                userId, startDate, endDate)
                : transactionRepository.findByUserIdOrderByTransactionDateDesc(userId);

        StringBuilder csv = new StringBuilder("Date,Description,Category,Amount,Type,Merchant,Notes\n");

        for (Transaction t : transactions) {
            csv.append(t.getTransactionDate()).append(',')
                    .append(esc(t.getDescription())).append(',')
                    .append(esc(t.getCategory().getName())).append(',')
                    .append(t.getAmount()).append(',')
                    .append(t.getType()).append(',')
                    .append(esc(t.getMerchant())).append(',')
                    .append(esc(t.getNotes())).append('\n');
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ==================== HELPERS ====================

    private Long getUserId(Authentication authentication) {
        return ((UserPrincipal) authentication.getPrincipal()).getId();
    }

    private BigDecimal sum(List<Transaction> list) {
        return list.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal max(List<Transaction> list) {
        return list.stream().map(Transaction::getAmount).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
    }

    private Double percentChange(BigDecimal oldVal, BigDecimal newVal) {
        if (oldVal == null || oldVal.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return newVal.subtract(oldVal)
                .divide(oldVal, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private String esc(String value) {
        if (value == null) return "";
        return value.contains(",") ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }

    private TransactionResponse mapToResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .category(CategoryResponse.builder()
                        .id(t.getCategory().getId())
                        .name(t.getCategory().getName())
                        .description(t.getCategory().getDescription())
                        .icon(t.getCategory().getIcon())
                        .color(t.getCategory().getColor())
                        .type(t.getCategory().getType())
                        .build())
                .description(t.getDescription())
                .amount(t.getAmount())
                .type(t.getType())
                .transactionDate(t.getTransactionDate())
                .merchant(t.getMerchant())
                .notes(t.getNotes())
                .isRecurring(t.getIsRecurring())
                .createdAt(t.getCreatedAt())
                .build();
    }
}