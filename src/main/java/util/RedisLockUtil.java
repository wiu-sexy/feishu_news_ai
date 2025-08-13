package com.example.feishuai.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Component
public class RedisLockUtil {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String LOCK_PREFIX = "feishu:lock:";
    private static final String UNLOCK_SCRIPT = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "   return redis.call('del', KEYS[1]) " +
        "else " +
        "   return 0 " +
        "end";
    
    private static final String RENEW_SCRIPT = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "   return redis.call('expire', KEYS[1], ARGV[2]) " +
        "else " +
        "   return 0 " +
        "end";

    public boolean tryLock(String key, String requestId, long expireTime, long waitTime, TimeUnit unit) {
        String fullKey = LOCK_PREFIX + key;
        long endTime = System.currentTimeMillis() + unit.toMillis(waitTime);
        
        while (System.currentTimeMillis() < endTime) {
            if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(fullKey, requestId, expireTime, TimeUnit.SECONDS))) {
                return true;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return false;
    }

    public boolean unlock(String key, String requestId) {
        String fullKey = LOCK_PREFIX + key;
        RedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(fullKey), requestId);
        return result != null && result == 1;
    }

    public boolean renewLock(String key, String requestId, long expireTime) {
        String fullKey = LOCK_PREFIX + key;
        RedisScript<Long> script = new DefaultRedisScript<>(RENEW_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(fullKey), requestId, String.valueOf(expireTime));
        return result != null && result == 1;
    }
}