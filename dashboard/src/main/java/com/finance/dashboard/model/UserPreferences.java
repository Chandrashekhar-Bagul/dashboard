package com.finance.dashboard.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String currency = "USD";

    @Column(nullable = false)
    private String language = "en";

    @Column(nullable = false)
    private String theme = "light"; // light, dark

    @Column(nullable = false)
    private Boolean emailNotifications = true;

    @Column(nullable = false)
    private Boolean budgetAlerts = true;

    @Column(nullable = false)
    private String dateFormat = "MM/DD/YYYY";
}