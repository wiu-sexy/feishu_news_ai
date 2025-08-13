package com.example.feishuai.service;

import com.example.feishuai.model.AIResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import io.github.resilience4j.decorators.Decorators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Service
public class AIService {
    
    @Value("${ai.service.url}")
    private String aiServiceUrl;
    
    @Value("${ai.callback.url}")
    private String callbackUrl;
    
    @Autowired
    private CircuitBreaker circuitBreaker;
    
    @Autowired
    private AlertService alertService;
    
    @Autowired
    private BaseBatchService baseBatchService;
    
    @Async
    public void asyncProcessMessage(String messageId, String content, String chatId) {
        Map<String, Object> request = Map.of(
            "message_id", messageId,
            "content", content,
            "chat_id", chatId,
            "callback_url", callbackUrl
        );
        
        Supplier<Void> supplier = () -> {
            new RestTemplate().postForEntity(aiServiceUrl, request, Void.class);
            return null;
        };
        
        Supplier<Void> decoratedSupplier = Decorators.ofSupplier(supplier)
            .withCircuitBreaker(circuitBreaker)
            .decorate();
        
        try {
            decoratedSupplier.get();
        } catch (CircuitBreakerOpenException e) {
            alertService.sendCircuitBreakerOpenAlert();
            fallbackProcessing(messageId, content);
        } catch (Exception e) {
            handleAIError(messageId, e);
        }
    }
    
    public void handleAICallback(Map<String, Object> callbackData) {
        AIResponse response = new AIResponse();
        response.setMessageId((String) callbackData.get("message_id"));
        response.setStatus((String) callbackData.get("status"));
        response.setResult((Map<String, String>) callbackData.get("result"));
        
        if ("success".equals(response.getStatus())) {
            baseBatchService.addRecord(new BaseRecord(response.getMessageId(), response.getResult()));
        } else {
            System.err.println("AI processing failed for message: " + response.getMessageId());
        }
    }
    
    public boolean validateAISignature(String signature, Map<String, Object> payload) {
        // 实现AI回调的签名验证
        return true;
    }
    
    private void handleAIError(String messageId, Exception e) {
        System.err.println("AI processing failed for message: " + messageId);
        e.printStackTrace();
        
        if (e.getCause() instanceof TimeoutException) {
            alertService.sendTimeoutAlert(messageId);
        }
        
        fallbackProcessing(messageId, "Fallback content");
    }
    
    private void fallbackProcessing(String messageId, String content) {
        baseBatchService.addRecord(new BaseRecord(messageId, Map.of(
            "problem_description", content,
            "category", "manual",
            "tester_id", "default_tester"
        )));
    }
}