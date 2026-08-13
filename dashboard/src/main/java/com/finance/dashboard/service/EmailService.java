package com.finance.dashboard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendWelcomeEmail(String to, String name) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Welcome to Finance Dashboard!");
        message.setText("Hello " + name + ",\n\nWelcome to your personal finance tracker.");
        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Email failed", e);
        }
    }

    public void sendPasswordResetEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Password Reset Request");
        message.setText("Click here to reset: http://localhost:3000/reset-password?token=" + token);
        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Email failed", e);
        }
    }
}