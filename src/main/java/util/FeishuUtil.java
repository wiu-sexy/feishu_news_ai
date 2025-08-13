package com.example.feishuai.util;

import com.example.feishuai.config.FeishuConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class FeishuUtil {
    
    @Autowired
    private FeishuConfig feishuConfig;
    
    private String tenantAccessToken;
    private long tokenExpireTime;
    
    public String getTenantAccessToken() {
        if (System.currentTimeMillis() < tokenExpireTime) {
            return tenantAccessToken;
        }
        
        String url = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal/";
        Map<String, String> request = Map.of(
            "app_id", feishuConfig.getAppId(),
            "app_secret", feishuConfig.getAppSecret()
        );
        
        ResponseEntity<Map> response = new RestTemplate().postForEntity(url, request, Map.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> body = response.getBody();
            tenantAccessToken = (String) body.get("tenant_access_token");
            tokenExpireTime = System.currentTimeMillis() + ((Integer) body.get("expire")) * 1000;
            return tenantAccessToken;
        }
        throw new RuntimeException("Failed to get tenant access token");
    }
}