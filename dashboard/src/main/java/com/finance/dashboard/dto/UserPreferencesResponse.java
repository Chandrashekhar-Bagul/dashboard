package com.finance.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesResponse {
    private Long id;
    private String currency;
    private String language;
    private String theme;
    private Boolean emailNotifications;
    private Boolean budgetAlerts;
    private String dateFormat;
}