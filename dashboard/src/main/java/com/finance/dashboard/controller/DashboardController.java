package com.finance.dashboard.controller;

import com.finance.dashboard.dto.AIInsightResponse;
import com.finance.dashboard.dto.DashboardStats;
import com.finance.dashboard.security.UserPrincipal;
import com.finance.dashboard.service.AIInsightService;
import com.finance.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final AIInsightService insightService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getDashboardStats(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        LocalDate now = LocalDate.now();

        int selectedMonth =
                month != null ? month : now.getMonthValue();

        int selectedYear =
                year != null ? year : now.getYear();

        DashboardStats stats =
                dashboardService.getDashboardStats(
                        userId,
                        selectedMonth,
                        selectedYear
                );

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/insights")
    public ResponseEntity<List<AIInsightResponse>> getUnreadInsights(
            Authentication authentication) {

        Long userId = getUserId(authentication);

        return ResponseEntity.ok(
                insightService.getUserInsights(userId, false)
        );
    }

    @PatchMapping("/insights/{id}/read")
    public ResponseEntity<Void> markInsightAsRead(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = getUserId(authentication);

        insightService.markInsightAsRead(id, userId);

        return ResponseEntity.noContent().build();
    }

    private Long getUserId(Authentication authentication) {

        UserPrincipal principal =
                (UserPrincipal) authentication.getPrincipal();

        return principal.getId();
    }
}