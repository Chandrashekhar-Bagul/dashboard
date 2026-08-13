package com.finance.dashboard.controller;

import com.finance.dashboard.dto.*;
import com.finance.dashboard.security.UserPrincipal;
import com.finance.dashboard.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins}")
public class UserController {

    private  UserService userService;

    /**
     * Get current user profile
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            Authentication authentication) {

        log.info("Fetching profile for user: {}", getUserId(authentication));

        UserProfileResponse profile = userService.getUserProfile(getUserId(authentication));

        return ResponseEntity.ok(ApiResponse.<UserProfileResponse>builder()
                .success(true)
                .message("Profile retrieved successfully")
                .data(profile)
                .build());
    }

    /**
     * Update user profile
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {

        log.info("Updating profile for user: {}", getUserId(authentication));

        UserProfileResponse profile = userService.updateProfile(
                getUserId(authentication), request);

        return ResponseEntity.ok(ApiResponse.<UserProfileResponse>builder()
                .success(true)
                .message("Profile updated successfully")
                .data(profile)
                .build());
    }

    /**
     * Change password
     */
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {

        log.info("Changing password for user: {}", getUserId(authentication));

        userService.changePassword(getUserId(authentication), request);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Password changed successfully")
                .build());
    }

    /**
     * Get user preferences
     */
    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<UserPreferencesResponse>> getPreferences(
            Authentication authentication) {

        log.info("Fetching preferences for user: {}", getUserId(authentication));

        UserPreferencesResponse preferences = userService.getUserPreferences(
                getUserId(authentication));

        return ResponseEntity.ok(ApiResponse.<UserPreferencesResponse>builder()
                .success(true)
                .message("Preferences retrieved successfully")
                .data(preferences)
                .build());
    }

    /**
     * Update user preferences
     */
    @PutMapping("/preferences")
    public ResponseEntity<ApiResponse<UserPreferencesResponse>> updatePreferences(
            @Valid @RequestBody UserPreferencesRequest request,
            Authentication authentication) {

        log.info("Updating preferences for user: {}", getUserId(authentication));

        UserPreferencesResponse preferences = userService.updatePreferences(
                getUserId(authentication), request);

        return ResponseEntity.ok(ApiResponse.<UserPreferencesResponse>builder()
                .success(true)
                .message("Preferences updated successfully")
                .data(preferences)
                .build());
    }

    /**
     * Delete account
     */
    @DeleteMapping("/account")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @RequestBody DeleteAccountRequest request,
            Authentication authentication) {

        log.info("Deleting account for user: {}", getUserId(authentication));

        userService.deleteAccount(getUserId(authentication), request.getPassword());

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Account deleted successfully")
                .build());
    }

    private Long getUserId(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return principal.getId();
    }
}