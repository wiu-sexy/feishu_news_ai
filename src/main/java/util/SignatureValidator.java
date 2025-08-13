package com.example.feishuai.util;

import org.apache.commons.codec.digest.HmacUtils;

public class SignatureValidator {
    public static boolean validate(String signature, String timestamp, String nonce, String body, String token) {
        try {
            String data = timestamp + nonce + token + body;
            String computedSignature = "v0=" + HmacUtils.hmacSha256Hex(token, data);
            return computedSignature.equals(signature);
        } catch (Exception e) {
            System.err.println("Signature validation failed: " + e.getMessage());
            return false;
        }
    }
}