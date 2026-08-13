package com.finance.dashboard.repository;

import com.finance.dashboard.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUserIdAndMonthAndYear(Long userId, Integer month, Integer year);

    Optional<Budget> findByUserIdAndCategoryIdAndMonthAndYear(
            Long userId, Long categoryId, Integer month, Integer year);
    List<Budget> findByUserId(Long userId);

    List<Budget> findByUserIdAndCategoryId(Long userId, Long categoryId);
    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId " +
            "AND b.year = :year " +
            "ORDER BY b.month DESC, b.category.name")
    List<Budget> findByUserIdAndYear(@Param("userId") Long userId, @Param("year") Integer year);

    void deleteByUserId(Long userId);
}