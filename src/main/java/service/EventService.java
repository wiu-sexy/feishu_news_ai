package com.example.feishuai.service;

import com.example.feishuai.model.FeishuEvent;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EventService {
    
    @Autowired
    private AIService aiService;
    
    @Autowired
    private LockService lockService;
    
    @Async
    public void processMessageEvent(JsonNode event) {
        String messageId = event.path("message").path("message_id").asText();
        String requestId = UUID.randomUUID().toString();
        
        lockService.executeWithLock(messageId, 5, () -> {
            if (messageProcessed(messageId)) {
                return null;
            }
            
            String chatId = event.path("message").path("chat_id").asText();
            String content = event.path("message").path("content").asText();
            String cleanContent = content.replaceAll("<[^>]+>", "");
            
            aiService.asyncProcessMessage(messageId, cleanContent, chatId);
            return null;
        });
    }
    
    private boolean messageProcessed(String messageId) {
        // 实现检查消息是否已处理的逻辑
        return false;
    }
}