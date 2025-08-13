package com.example.feishuai.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class BaseRecord {
    private String messageId;
    private Map<String, Object> fields;
}