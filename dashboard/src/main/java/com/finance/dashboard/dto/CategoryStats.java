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
public class CategoryStats {
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;
    private BigDecimal totalSpent;
    private BigDecimal averageTransaction;
    private Integer transactionCount;
    private BigDecimal highestTransaction;
    private BigDecimal lowestTransaction;
    private Integer month;
    private Integer year;
}