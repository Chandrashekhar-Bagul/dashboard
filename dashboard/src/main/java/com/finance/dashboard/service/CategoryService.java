package com.finance.dashboard.service;

import com.finance.dashboard.dto.CategoryRequest;
import com.finance.dashboard.dto.CategoryResponse;
import com.finance.dashboard.dto.CategoryStats;
import com.finance.dashboard.model.Category;
import com.finance.dashboard.model.Category.CategoryType;
import com.finance.dashboard.model.Transaction;
import com.finance.dashboard.repository.CategoryRepository;
import com.finance.dashboard.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'all'")
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "#type")
    public List<CategoryResponse> getCategoriesByType(String type) {
        CategoryType categoryType = CategoryType.valueOf(type.toUpperCase());
        return categoryRepository.findByType(categoryType)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return mapToResponse(category);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.findByName(request.getName()).isPresent()) {
            throw new RuntimeException("Category with this name already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .icon(request.getIcon())
                .color(request.getColor())
                .type(request.getType())
                .build();

        category = categoryRepository.save(category);
        return mapToResponse(category);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Check if name is being changed to an existing name
        if (!category.getName().equals(request.getName())) {
            categoryRepository.findByName(request.getName()).ifPresent(c -> {
                throw new RuntimeException("Category with this name already exists");
            });
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setIcon(request.getIcon());
        category.setColor(request.getColor());
        category.setType(request.getType());

        category = categoryRepository.save(category);
        return mapToResponse(category);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Check if category has transactions
        long transactionCount = transactionRepository.countByCategoryId(id);
        if (transactionCount > 0) {
            throw new RuntimeException("Cannot delete category with existing transactions");
        }

        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public CategoryStats getCategoryStats(Long id, Integer month, Integer year) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        LocalDate startDate, endDate;

        if (month != null && year != null) {
            YearMonth yearMonth = YearMonth.of(year, month);
            startDate = yearMonth.atDay(1);
            endDate = yearMonth.atEndOfMonth();
        } else {
            // Default to current month
            YearMonth currentMonth = YearMonth.now();
            startDate = currentMonth.atDay(1);
            endDate = currentMonth.atEndOfMonth();
            month = currentMonth.getMonthValue();
            year = currentMonth.getYear();
        }

        List<Transaction> transactions = transactionRepository
                .findByCategoryIdAndTransactionDateBetween(id, startDate, endDate);

        BigDecimal totalSpent = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageTransaction = transactions.isEmpty()
                ? BigDecimal.ZERO
                : totalSpent.divide(BigDecimal.valueOf(transactions.size()), 2, BigDecimal.ROUND_HALF_UP);

        BigDecimal highest = transactions.stream()
                .map(Transaction::getAmount)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal lowest = transactions.stream()
                .map(Transaction::getAmount)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        return CategoryStats.builder()
                .categoryId(category.getId())
                .categoryName(category.getName())
                .categoryIcon(category.getIcon())
                .categoryColor(category.getColor())
                .totalSpent(totalSpent)
                .averageTransaction(averageTransaction)
                .transactionCount(transactions.size())
                .highestTransaction(highest)
                .lowestTransaction(lowest)
                .month(month)
                .year(year)
                .build();
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .icon(category.getIcon())
                .color(category.getColor())
                .type(category.getType())
                .build();
    }
}