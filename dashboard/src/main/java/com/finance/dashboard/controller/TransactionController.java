package com.finance.dashboard.controller;

import com.finance.dashboard.dto.*;
import com.finance.dashboard.security.UserPrincipal;
import com.finance.dashboard.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins}")
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Create a new transaction
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody TransactionRequest request,
            Authentication authentication) {

        log.info("Creating transaction for user: {}", getUserId(authentication));

        TransactionResponse response = transactionService.createTransaction(request, authentication);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<TransactionResponse>builder()
                        .success(true)
                        .message("Transaction created successfully")
                        .data(response)
                        .build());
    }

    /**
     * Get all transactions for current user with pagination
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getAllTransactions(
            @PageableDefault(size = 20, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {

        log.info("Fetching transactions for user: {}", getUserId(authentication));

        Page<TransactionResponse> transactions = transactionService.getUserTransactionsPaginated(
                getUserId(authentication), pageable);

        return ResponseEntity.ok(ApiResponse.<Page<TransactionResponse>>builder()
                .success(true)
                .message("Transactions retrieved successfully")
                .data(transactions)
                .build());
    }

    /**
     * Get transaction by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionById(
            @PathVariable Long id,
            Authentication authentication) {

        log.info("Fetching transaction {} for user: {}", id, getUserId(authentication));

        TransactionResponse transaction = transactionService.getTransaction(id, authentication);

        return ResponseEntity.ok(ApiResponse.<TransactionResponse>builder()
                .success(true)
                .message("Transaction retrieved successfully")
                .data(transaction)
                .build());
    }

    /**
     * Get transactions by date range
     */
    @GetMapping("/range")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionsByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            Authentication authentication) {

        log.info("Fetching transactions from {} to {} for user: {}",
                startDate, endDate, getUserId(authentication));

        List<TransactionResponse> transactions = transactionService.getTransactionsByDateRange(
                getUserId(authentication), startDate, endDate);

        return ResponseEntity.ok(ApiResponse.<List<TransactionResponse>>builder()
                .success(true)
                .message("Transactions retrieved successfully")
                .data(transactions)
                .build());
    }

    /**
     * Get transactions by category
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Authentication authentication) {

        log.info("Fetching transactions for category {} for user: {}",
                categoryId, getUserId(authentication));

        List<TransactionResponse> transactions = transactionService.getTransactionsByCategory(
                getUserId(authentication), categoryId, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.<List<TransactionResponse>>builder()
                .success(true)
                .message("Category transactions retrieved successfully")
                .data(transactions)
                .build());
    }

    /**
     * Get transactions by type (INCOME/EXPENSE)
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionsByType(
            @PathVariable String type,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Authentication authentication) {

        log.info("Fetching {} transactions for user: {}", type, getUserId(authentication));

        List<TransactionResponse> transactions = transactionService.getTransactionsByType(
                getUserId(authentication), type, month, year);

        return ResponseEntity.ok(ApiResponse.<List<TransactionResponse>>builder()
                .success(true)
                .message(type + " transactions retrieved successfully")
                .data(transactions)
                .build());
    }

    /**
     * Search transactions by description or merchant
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> searchTransactions(
            @RequestParam String query,
            Authentication authentication) {

        log.info("Searching transactions with query: {} for user: {}", query, getUserId(authentication));

        List<TransactionResponse> transactions = transactionService.searchTransactions(
                getUserId(authentication), query);

        return ResponseEntity.ok(ApiResponse.<List<TransactionResponse>>builder()
                .success(true)
                .message("Search results retrieved successfully")
                .data(transactions)
                .build());
    }

    /**
     * Get recurring transactions
     */
    @GetMapping("/recurring")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getRecurringTransactions(
            Authentication authentication) {

        log.info("Fetching recurring transactions for user: {}", getUserId(authentication));

        List<TransactionResponse> transactions = transactionService.getRecurringTransactions(
                getUserId(authentication));

        return ResponseEntity.ok(ApiResponse.<List<TransactionResponse>>builder()
                .success(true)
                .message("Recurring transactions retrieved successfully")
                .data(transactions)
                .build());
    }

    /**
     * Update transaction
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request,
            Authentication authentication) {

        log.info("Updating transaction {} for user: {}", id, getUserId(authentication));

        TransactionResponse response = transactionService.updateTransaction(id, request, authentication);

        return ResponseEntity.ok(ApiResponse.<TransactionResponse>builder()
                .success(true)
                .message("Transaction updated successfully")
                .data(response)
                .build());
    }

    /**
     * Delete transaction
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(
            @PathVariable Long id,
            Authentication authentication) {

        log.info("Deleting transaction {} for user: {}", id, getUserId(authentication));

        transactionService.deleteTransaction(id, authentication);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Transaction deleted successfully")
                .build());
    }

    /**
     * Bulk delete transactions
     */
    @DeleteMapping("/bulk")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> bulkDeleteTransactions(
            @RequestBody List<Long> transactionIds,
            Authentication authentication) {

        log.info("Bulk deleting {} transactions for user: {}",
                transactionIds.size(), getUserId(authentication));

        int deletedCount = transactionService.bulkDeleteTransactions(transactionIds, authentication);

        Map<String, Integer> result = new HashMap<>();
        result.put("deletedCount", deletedCount);

        return ResponseEntity.ok(ApiResponse.<Map<String, Integer>>builder()
                .success(true)
                .message(deletedCount + " transactions deleted successfully")
                .data(result)
                .build());
    }

    /**
     * Upload transactions from CSV file
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadTransactions(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        log.info("Uploading transactions file for user: {}", getUserId(authentication));

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("File is empty")
                            .build());
        }

        Map<String, Object> result = transactionService.uploadTransactionsFromCSV(
                file, getUserId(authentication));

        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("Transactions uploaded successfully")
                .data(result)
                .build());
    }

    /**
     * Export transactions to CSV
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportTransactions(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Authentication authentication) {

        log.info("Exporting transactions for user: {}", getUserId(authentication));

        byte[] csvData = transactionService.exportTransactionsToCSV(
                getUserId(authentication), startDate, endDate);

        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=transactions.csv")
                .body(csvData);
    }

    /**
     * Get transaction statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<TransactionStats>> getTransactionStats(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Authentication authentication) {

        log.info("Fetching transaction stats for user: {}", getUserId(authentication));

        TransactionStats stats = transactionService.getTransactionStats(
                getUserId(authentication), month, year);

        return ResponseEntity.ok(ApiResponse.<TransactionStats>builder()
                .success(true)
                .message("Transaction statistics retrieved successfully")
                .data(stats)
                .build());
    }

    /**
     * Get monthly comparison
     */
    @GetMapping("/compare")
    public ResponseEntity<ApiResponse<MonthlyComparison>> compareMonths(
            @RequestParam Integer month1,
            @RequestParam Integer year1,
            @RequestParam Integer month2,
            @RequestParam Integer year2,
            Authentication authentication) {

        log.info("Comparing months {}/{} vs {}/{} for user: {}",
                month1, year1, month2, year2, getUserId(authentication));

        MonthlyComparison comparison = transactionService.compareMonths(
                getUserId(authentication), month1, year1, month2, year2);

        return ResponseEntity.ok(ApiResponse.<MonthlyComparison>builder()
                .success(true)
                .message("Monthly comparison retrieved successfully")
                .data(comparison)
                .build());
    }

    private Long getUserId(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return principal.getId();
    }
}