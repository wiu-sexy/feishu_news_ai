package com.example.feishuai.model;

import lombok.Data;

import java.util.Map;

@Data
public class AIResponse {
    private String messageId;
    private String status;
    private Map<String, String> result;
}