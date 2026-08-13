package com.finance.dashboard.dto;

import com.finance.dashboard.model.AIInsight.InsightType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIInsightResponse {
    private Long id;
    private String insight;
    private InsightType type;
    private Boolean isRead;
    private LocalDateTime createdAt;
}