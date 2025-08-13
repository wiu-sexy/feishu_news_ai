package com.example.feishuai.service;

import com.example.feishuai.util.RedisLockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class LockService {
    
    @Autowired
    private RedisLockUtil redisLockUtil;
    
    @Value("${redis.lock.default-expire:30}")
    private long defaultExpireSeconds;
    
    public <T> T executeWithLock(String lockKey, long waitSeconds, Supplier<T> supplier) {
        String requestId = UUID.randomUUID().toString();
        try {
            if (redisLockUtil.tryLock(lockKey, requestId, defaultExpireSeconds, waitSeconds, TimeUnit.SECONDS)) {
                return supplier.get();
            }
            throw new RuntimeException("Failed to acquire lock for: " + lockKey);
        } finally {
            redisLockUtil.unlock(lockKey, requestId);
        }
    }
}