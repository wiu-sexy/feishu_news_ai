package com.example.feishuai.service;

import org.springframework.stereotype.Service;

@Service
public class AlertService {
    
    public void sendCircuitBreakerOpenAlert() {
        System.err.println("ALERT: Circuit breaker is OPEN! AI service unavailable.");
        // 实际项目中应发送邮件、短信或Slack通知
    }
    
    public void sendTimeoutAlert(String messageId) {
        System.err.println("ALERT: AI processing timeout for message: " + messageId);
        // 实际项目中应发送邮件、短信或Slack通知
    }
}