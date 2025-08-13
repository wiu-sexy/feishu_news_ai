package com.example.feishuai.controller;

import com.example.feishuai.service.BaseBatchService;
import com.example.feishuai.util.RedisLockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class HealthController {
    
    @Autowired
    private BaseBatchService batchService;
    
    @Autowired
    private RedisLockUtil redisLockUtil;
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> details = new LinkedHashMap<>();
        
        // 系统状态
        details.put("status", "UP");
        details.put("version", "1.0.0");
        
        // 组件健康
        details.put("components", Map.of(
            "batchQueue", batchService.getQueueStatus(),
            "redisLock", testRedisLock()
        ));
        
        return ResponseEntity.ok(details);
    }
    
    private Map<String, Object> testRedisLock() {
        String testKey = "health-check-lock";
        String requestId = UUID.randomUUID().toString();
        
        try {
            boolean acquired = redisLockUtil.tryLock(testKey, requestId, 10);
            if (acquired) {
                return Map.of("status", "OK", "message", "Lock acquired successfully");
            }
            return Map.of("status", "DOWN", "message", "Failed to acquire lock");
        } finally {
            redisLockUtil.unlock(testKey, requestId);
        }
    }
}