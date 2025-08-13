package com.example.feishuai.controller;

import com.example.feishuai.util.JsonUtil;
import com.example.feishuai.util.SignatureValidator;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/feishu")
public class EventController {
    
    @Value("${feishu.verification-token}")
    private String verificationToken;
    
    @Autowired
    private EventService eventService;
    
    @PostMapping("/event")
    public Object handleEvent(
            @RequestHeader("X-Lark-Signature") String signature,
            @RequestHeader("X-Lark-Request-Timestamp") String timestamp,
            @RequestHeader("X-Lark-Request-Nonce") String nonce,
            @RequestBody String body) {
        
        // 1. 验证签名
        if (!SignatureValidator.validate(signature, timestamp, nonce, body, verificationToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // 2. 解析事件类型
        JsonNode eventData = JsonUtil.parse(body);
        String type = eventData.path("type").asText();
        
        // 3. 处理URL验证事件
        if ("url_verification".equals(type)) {
            return Map.of("challenge", eventData.path("challenge").asText());
        }
        
        // 4. 处理消息事件
        if ("event_callback".equals(type)) {
            JsonNode event = eventData.path("event");
            if ("im.message.receive_v1".equals(event.path("type").asText())) {
                eventService.processMessageEvent(event);
            }
        }
        
        return Map.of("code", 0);
    }
}