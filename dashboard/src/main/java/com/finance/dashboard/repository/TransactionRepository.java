package com.finance.dashboard.repository;

import com.finance.dashboard.model.Transaction;
import com.finance.dashboard.model.Transaction.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserIdOrderByTransactionDateDesc(Long userId);

    Page<Transaction> findByUserId(Long userId, Pageable pageable);

    List<Transaction> findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            Long userId, LocalDate startDate, LocalDate endDate);

    List<Transaction> findByUserIdAndCategoryId(Long userId, Long categoryId);

    List<Transaction> findByUserIdAndCategoryIdAndTransactionDateBetween(
            Long userId, Long categoryId, LocalDate startDate, LocalDate endDate);

    List<Transaction> findByUserIdAndType(Long userId, TransactionType type);

    List<Transaction> findByUserIdAndIsRecurring(Long userId, Boolean isRecurring);

    List<Transaction> findTop5ByUserIdAndCategoryIdOrderByTransactionDateDesc(
            Long userId, Long categoryId);

    List<Transaction> findByCategoryIdAndTransactionDateBetween(
            Long categoryId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND (" +
            "LOWER(t.description) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(t.merchant, '')) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "ORDER BY t.transactionDate DESC")
    List<Transaction> searchByKeyword(@Param("userId") Long userId, @Param("q") String q);

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
            "AND t.type = :type " +
            "AND YEAR(t.transactionDate) = :year " +
            "AND MONTH(t.transactionDate) = :month " +
            "ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserAndTypeAndMonthYear(@Param("userId") Long userId,
                                                    @Param("type") TransactionType type,
                                                    @Param("year") int year,
                                                    @Param("month") int month);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.user.id = :userId AND t.category.id = :categoryId " +
            "AND YEAR(t.transactionDate) = :year AND MONTH(t.transactionDate) = :month")
    BigDecimal sumAmountByCategoryAndMonthYear(@Param("userId") Long userId,
                                               @Param("categoryId") Long categoryId,
                                               @Param("year") int year,
                                               @Param("month") int month);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.user.id = :userId AND t.type = :type " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumByUserAndTypeAndDateRange(@Param("userId") Long userId,
                                            @Param("type") TransactionType type,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    Long countByUserId(Long userId);

    Long countByCategoryId(Long categoryId);

    void deleteByUserId(Long userId);
}