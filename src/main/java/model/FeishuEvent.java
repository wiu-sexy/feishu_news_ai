package com.example.feishuai.model;

import lombok.Data;

@Data
public class FeishuEvent {
    private String type;
    private String challenge;
    private Event event;
    
    @Data
    public static class Event {
        private String type;
        private String app_id;
        private String tenant_key;
        private Message message;
        
        @Data
        public static class Message {
            private String message_id;
            private String chat_id;
            private String content;
            private String message_type;
            private long create_time;
        }
    }
}