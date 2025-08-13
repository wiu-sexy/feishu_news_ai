package com.example.feishuai.exception;

public class AsyncProcessingException extends RuntimeException {
    public AsyncProcessingException(String message) {
        super(message);
    }

    public AsyncProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}