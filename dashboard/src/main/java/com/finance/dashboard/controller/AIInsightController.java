package com.finance.dashboard.controller;

import com.finance.dashboard.dto.*;
import com.finance.dashboard.security.UserPrincipal;
import com.finance.dashboard.service.AIInsightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins}")
public class AIInsightController {

    private AIInsightService aiInsightService;

    /**
     * Get all insights for current user
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AIInsightResponse>>> getAllInsights(
            @RequestParam(required = false, defaultValue = "false") Boolean unreadOnly,
            Authentication authentication) {

        log.info("Fetching insights for user: {}", getUserId(authentication));

        List<AIInsightResponse> insights = aiInsightService.getUserInsights(
                getUserId(authentication), unreadOnly ? false : null);

        return ResponseEntity.ok(ApiResponse.<List<AIInsightResponse>>builder()
                .success(true)
                .message("Insights retrieved successfully")
                .data(insights)
                .build());
    }

    /**
     * Get insights by type
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<List<AIInsightResponse>>> getInsightsByType(
            @PathVariable String type,
            Authentication authentication) {

        log.info("Fetching {} insights for user: {}", type, getUserId(authentication));

        List<AIInsightResponse> insights = aiInsightService.getInsightsByType(
                getUserId(authentication), type);

        return ResponseEntity.ok(ApiResponse.<List<AIInsightResponse>>builder()
                .success(true)
                .message("Insights retrieved successfully")
                .data(insights)
                .build());
    }

    /**
     * Generate new insights
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> generateInsights(
            Authentication authentication) {

        log.info("Generating insights for user: {}", getUserId(authentication));

        int generatedCount = aiInsightService.generateAllInsights(getUserId(authentication));

        return ResponseEntity.ok(ApiResponse.<Map<String, Integer>>builder()
                .success(true)
                .message(generatedCount + " insights generated successfully")
                .data(Map.of("generatedCount", generatedCount))
                .build());
    }

    /**
     * Mark insight as read
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {

        log.info("Marking insight {} as read for user: {}", id, getUserId(authentication));

        aiInsightService.markInsightAsRead(id, getUserId(authentication));

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Insight marked as read")
                .build());
    }

    /**
     * Mark all insights as read
     */
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead(
            Authentication authentication) {

        log.info("Marking all insights as read for user: {}", getUserId(authentication));

        int markedCount = aiInsightService.markAllInsightsAsRead(getUserId(authentication));

        return ResponseEntity.ok(ApiResponse.<Map<String, Integer>>builder()
                .success(true)
                .message(markedCount + " insights marked as read")
                .data(Map.of("markedCount", markedCount))
                .build());
    }

    /**
     * Delete insight
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInsight(
            @PathVariable Long id,
            Authentication authentication) {

        log.info("Deleting insight {} for user: {}", id, getUserId(authentication));

        aiInsightService.deleteInsight(id, getUserId(authentication));

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Insight deleted successfully")
                .build());
    }

    /**
     * Get unread count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            Authentication authentication) {

        Long unreadCount = aiInsightService.getUnreadCount(getUserId(authentication));

        return ResponseEntity.ok(ApiResponse.<Map<String, Long>>builder()
                .success(true)
                .message("Unread count retrieved successfully")
                .data(Map.of("unreadCount", unreadCount))
                .build());
    }

    private Long getUserId(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return principal.getId();
    }
}