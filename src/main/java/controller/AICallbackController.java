package com.example.feishuai.controller;

import com.example.feishuai.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/feishu")
public class AICallbackController {
    
    @Autowired
    private AIService aiService;
    
    @PostMapping("/ai-callback")
    public ResponseEntity<?> handleAICallback(
            @RequestHeader("X-AI-Signature") String signature,
            @RequestBody Map<String, Object> payload) {
        
        // 1. 验证AI服务签名
        if (!aiService.validateAISignature(signature, payload)) {
            return ResponseEntity.status(403).build();
        }
        
        // 2. 处理回调数据
        aiService.handleAICallback(payload);
        return ResponseEntity.ok().build();
    }
}