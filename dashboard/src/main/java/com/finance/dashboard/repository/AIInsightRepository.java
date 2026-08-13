package com.finance.dashboard.repository;

import com.finance.dashboard.model.AIInsight;
import com.finance.dashboard.model.AIInsight.InsightType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIInsightRepository extends JpaRepository<AIInsight, Long> {

    List<AIInsight> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<AIInsight> findByUserIdAndIsReadOrderByCreatedAtDesc(Long userId, Boolean isRead);

    List<AIInsight> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, InsightType type);

    Long countByUserIdAndIsRead(Long userId, Boolean isRead);

    void deleteByUserId(Long userId);
}