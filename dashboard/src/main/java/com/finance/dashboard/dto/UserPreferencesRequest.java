package com.finance.dashboard.dto;

import lombok.Data;

@Data
public class UserPreferencesRequest {
    private String currency;
    private String language;
    private String theme;
    private Boolean emailNotifications;
    private Boolean budgetAlerts;
    private String dateFormat;
}