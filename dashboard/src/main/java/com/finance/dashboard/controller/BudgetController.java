package com.finance.dashboard.controller;

import com.finance.dashboard.dto.*;
import com.finance.dashboard.security.UserPrincipal;
import com.finance.dashboard.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins}")
public class BudgetController {

    private BudgetService budgetService;

    /**
     * Create a new budget
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BudgetResponse>> createBudget(
            @Valid @RequestBody BudgetRequest request,
            Authentication authentication) {

        log.info("Creating budget for user: {}", getUserId(authentication));

        BudgetResponse response = budgetService.createBudget(request, authentication);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<BudgetResponse>builder()
                        .success(true)
                        .message("Budget created successfully")
                        .data(response)
                        .build());
    }

    /**
     * Get all budgets for current user
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getAllBudgets(
            Authentication authentication) {

        log.info("Fetching all budgets for user: {}", getUserId(authentication));

        List<BudgetResponse> budgets = budgetService.getAllUserBudgets(getUserId(authentication));

        return ResponseEntity.ok(ApiResponse.<List<BudgetResponse>>builder()
                .success(true)
                .message("Budgets retrieved successfully")
                .data(budgets)
                .build());
    }

    /**
     * Get budget by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BudgetResponse>> getBudgetById(
            @PathVariable Long id,
            Authentication authentication) {

        log.info("Fetching budget {} for user: {}", id, getUserId(authentication));

        BudgetResponse budget = budgetService.getBudgetById(id, authentication);

        return ResponseEntity.ok(ApiResponse.<BudgetResponse>builder()
                .success(true)
                .message("Budget retrieved successfully")
                .data(budget)
                .build());
    }

    /**
     * Get budgets for specific month and year
     */
    @GetMapping("/month/{month}/year/{year}")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getBudgetsByMonthYear(
            @PathVariable Integer month,
            @PathVariable Integer year,
            Authentication authentication) {

        log.info("Fetching budgets for {}/{} for user: {}", month, year, getUserId(authentication));

        List<BudgetResponse> budgets = budgetService.getBudgetsByMonthYear(
                getUserId(authentication), month, year);

        return ResponseEntity.ok(ApiResponse.<List<BudgetResponse>>builder()
                .success(true)
                .message("Monthly budgets retrieved successfully")
                .data(budgets)
                .build());
    }

    /**
     * Get current month budgets
     */
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getCurrentMonthBudgets(
            Authentication authentication) {

        log.info("Fetching current month budgets for user: {}", getUserId(authentication));

        List<BudgetResponse> budgets = budgetService.getCurrentMonthBudgets(
                getUserId(authentication));

        return ResponseEntity.ok(ApiResponse.<List<BudgetResponse>>builder()
                .success(true)
                .message("Current month budgets retrieved successfully")
                .data(budgets)
                .build());
    }

    /**
     * Get budgets by category
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getBudgetsByCategory(
            @PathVariable Long categoryId,
            Authentication authentication) {

        log.info("Fetching budgets for category {} for user: {}",
                categoryId, getUserId(authentication));

        List<BudgetResponse> budgets = budgetService.getBudgetsByCategory(
                getUserId(authentication), categoryId);

        return ResponseEntity.ok(ApiResponse.<List<BudgetResponse>>builder()
                .success(true)
                .message("Category budgets retrieved successfully")
                .data(budgets)
                .build());
    }

    /**
     * Get budgets for specific year
     */
    @GetMapping("/year/{year}")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getBudgetsByYear(
            @PathVariable Integer year,
            Authentication authentication) {

        log.info("Fetching budgets for year {} for user: {}", year, getUserId(authentication));

        List<BudgetResponse> budgets = budgetService.getBudgetsByYear(
                getUserId(authentication), year);

        return ResponseEntity.ok(ApiResponse.<List<BudgetResponse>>builder()
                .success(true)
                .message("Yearly budgets retrieved successfully")
                .data(budgets)
                .build());
    }

    /**
     * Get budget status with spending details
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<BudgetStatusDetail>> getBudgetStatus(
            @PathVariable Long id,
            Authentication authentication) {

        log.info("Fetching budget status for budget {} for user: {}",
                id, getUserId(authentication));

        BudgetStatusDetail status = budgetService.getBudgetStatusDetail(id, authentication);

        return ResponseEntity.ok(ApiResponse.<BudgetStatusDetail>builder()
                .success(true)
                .message("Budget status retrieved successfully")
                .data(status)
                .build());
    }

    /**
     * Get over-budget alerts
     */
    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getOverBudgetAlerts(
            Authentication authentication) {

        log.info("Fetching over-budget alerts for user: {}", getUserId(authentication));

        List<BudgetResponse> alerts = budgetService.getOverBudgetAlerts(
                getUserId(authentication));

        return ResponseEntity.ok(ApiResponse.<List<BudgetResponse>>builder()
                .success(true)
                .message("Budget alerts retrieved successfully")
                .data(alerts)
                .build());
    }

    /**
     * Update budget
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BudgetResponse>> updateBudget(
            @PathVariable Long id,
            @Valid @RequestBody BudgetRequest request,
            Authentication authentication) {

        log.info("Updating budget {} for user: {}", id, getUserId(authentication));

        BudgetResponse response = budgetService.updateBudget(id, request, authentication);

        return ResponseEntity.ok(ApiResponse.<BudgetResponse>builder()
                .success(true)
                .message("Budget updated successfully")
                .data(response)
                .build());
    }

    /**
     * Update budget alert settings
     */
    @PatchMapping("/{id}/alert-settings")
    public ResponseEntity<ApiResponse<BudgetResponse>> updateAlertSettings(
            @PathVariable Long id,
            @RequestBody BudgetAlertSettingsRequest request,
            Authentication authentication) {

        log.info("Updating alert settings for budget {} for user: {}",
                id, getUserId(authentication));

        BudgetResponse response = budgetService.updateAlertSettings(id, request, authentication);

        return ResponseEntity.ok(ApiResponse.<BudgetResponse>builder()
                .success(true)
                .message("Alert settings updated successfully")
                .data(response)
                .build());
    }

    /**
     * Delete budget
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(
            @PathVariable Long id,
            Authentication authentication) {

        log.info("Deleting budget {} for user: {}", id, getUserId(authentication));

        budgetService.deleteBudget(id, authentication);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Budget deleted successfully")
                .build());
    }

    /**
     * Clone budget to another month
     */
    @PostMapping("/{id}/clone")
    public ResponseEntity<ApiResponse<BudgetResponse>> cloneBudget(
            @PathVariable Long id,
            @RequestParam Integer targetMonth,
            @RequestParam Integer targetYear,
            Authentication authentication) {

        log.info("Cloning budget {} to {}/{} for user: {}",
                id, targetMonth, targetYear, getUserId(authentication));

        BudgetResponse response = budgetService.cloneBudget(
                id, targetMonth, targetYear, authentication);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<BudgetResponse>builder()
                        .success(true)
                        .message("Budget cloned successfully")
                        .data(response)
                        .build());
    }

    /**
     * Copy budgets from one month to another
     */
    @PostMapping("/copy")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> copyBudgets(
            @RequestParam Integer sourceMonth,
            @RequestParam Integer sourceYear,
            @RequestParam Integer targetMonth,
            @RequestParam Integer targetYear,
            Authentication authentication) {

        log.info("Copying budgets from {}/{} to {}/{} for user: {}",
                sourceMonth, sourceYear, targetMonth, targetYear, getUserId(authentication));

        List<BudgetResponse> budgets = budgetService.copyBudgets(
                getUserId(authentication), sourceMonth, sourceYear, targetMonth, targetYear);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<List<BudgetResponse>>builder()
                        .success(true)
                        .message(budgets.size() + " budgets copied successfully")
                        .data(budgets)
                        .build());
    }

    /**
     * Get budget summary
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<BudgetSummary>> getBudgetSummary(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Authentication authentication) {

        log.info("Fetching budget summary for user: {}", getUserId(authentication));

        BudgetSummary summary = budgetService.getBudgetSummary(
                getUserId(authentication), month, year);

        return ResponseEntity.ok(ApiResponse.<BudgetSummary>builder()
                .success(true)
                .message("Budget summary retrieved successfully")
                .data(summary)
                .build());
    }

    /**
     * Get budget trends (last N months)
     */
    @GetMapping("/trends")
    public ResponseEntity<ApiResponse<List<BudgetTrend>>> getBudgetTrends(
            @RequestParam(defaultValue = "6") Integer months,
            Authentication authentication) {

        log.info("Fetching budget trends for {} months for user: {}",
                months, getUserId(authentication));

        List<BudgetTrend> trends = budgetService.getBudgetTrends(
                getUserId(authentication), months);

        return ResponseEntity.ok(ApiResponse.<List<BudgetTrend>>builder()
                .success(true)
                .message("Budget trends retrieved successfully")
                .data(trends)
                .build());
    }

    /**
     * Get budget performance metrics
     */
    @GetMapping("/performance")
    public ResponseEntity<ApiResponse<BudgetPerformance>> getBudgetPerformance(
            @RequestParam Integer month,
            @RequestParam Integer year,
            Authentication authentication) {

        log.info("Fetching budget performance for {}/{} for user: {}",
                month, year, getUserId(authentication));

        BudgetPerformance performance = budgetService.getBudgetPerformance(
                getUserId(authentication), month, year);

        return ResponseEntity.ok(ApiResponse.<BudgetPerformance>builder()
                .success(true)
                .message("Budget performance retrieved successfully")
                .data(performance)
                .build());
    }

    /**
     * Adjust budget amount
     */
    @PatchMapping("/{id}/adjust")
    public ResponseEntity<ApiResponse<BudgetResponse>> adjustBudget(
            @PathVariable Long id,
            @RequestParam BigDecimal newAmount,
            @RequestParam(required = false) String reason,
            Authentication authentication) {

        log.info("Adjusting budget {} to {} for user: {}",
                id, newAmount, getUserId(authentication));

        BudgetResponse response = budgetService.adjustBudgetAmount(
                id, newAmount, reason, authentication);

        return ResponseEntity.ok(ApiResponse.<BudgetResponse>builder()
                .success(true)
                .message("Budget adjusted successfully")
                .data(response)
                .build());
    }

    private Long getUserId(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return principal.getId();
    }
}