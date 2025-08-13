package com.example.feishuai.service;

import com.example.feishuai.config.FeishuConfig;
import com.example.feishuai.exception.FeishuApiException;
import com.example.feishuai.model.BaseRecord;
import com.example.feishuai.util.FeishuUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BaseService {
    
    @Autowired
    private FeishuUtil feishuUtil;
    
    @Autowired
    private FeishuConfig feishuConfig;
    
    private static final String BASE_API = "https://open.feishu.cn/open-apis/bitable/v1/apps/%s/tables/%s/records";
    
    @Retryable(value = FeishuApiException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void createSingleRecord(BaseRecord record) {
        String token = feishuUtil.getTenantAccessToken();
        String url = String.format(BASE_API, feishuConfig.getBaseAppId(), feishuConfig.getTableId());
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> recordData = Map.of("fields", record.getFields());
        Map<String, Object> requestBody = Map.of("records", Collections.singletonList(recordData));
        
        ResponseEntity<Map> response = new RestTemplate().exchange(
            url, HttpMethod.POST, new HttpEntity<>(requestBody, headers), Map.class);
        
        if (!response.getStatusCode().is2xxSuccessful() || 
            !"0".equals(response.getBody().get("code").toString())) {
            throw new FeishuApiException("Failed to create base record: " + response.getBody());
        }
    }
    
    public void batchCreateRecords(List<BaseRecord> records) {
        if (records.isEmpty()) return;
        
        String token = feishuUtil.getTenantAccessToken();
        String url = String.format(BASE_API, feishuConfig.getBaseAppId(), feishuConfig.getTableId());
        
        List<Map<String, Object>> recordsData = records.stream()
            .map(record -> Map.of("fields", record.getFields()))
            .collect(Collectors.toList());
        
        Map<String, Object> requestBody = Map.of("records", recordsData);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        ResponseEntity<Map> response = new RestTemplate().exchange(
            url, HttpMethod.POST, new HttpEntity<>(requestBody, headers), Map.class);
        
        handleBatchResponse(response, records);
    }
    
    private void handleBatchResponse(ResponseEntity<Map> response, List<BaseRecord> records) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new FeishuApiException("Batch API failed with status: " + response.getStatusCode());
        }
        
        Map<String, Object> body = response.getBody();
        if (!"0".equals(body.get("code").toString())) {
            // 部分失败处理
            List<Map<String, Object>> failedRecords = (List<Map<String, Object>>) body.get("failed_records");
            if (failedRecords != null && !failedRecords.isEmpty()) {
                handlePartialFailure(records, failedRecords);
            } else {
                throw new FeishuApiException("Batch create failed: " + body);
            }
        }
    }
    
    private void handlePartialFailure(List<BaseRecord> originalRecords, List<Map<String, Object>> failedRecords) {
        Map<String, BaseRecord> recordMap = originalRecords.stream()
            .collect(Collectors.toMap(BaseRecord::getMessageId, record -> record));
        
        for (Map<String, Object> failed : failedRecords) {
            String messageId = (String) failed.get("record_id");
            String errorCode = failed.get("error_code").toString();
            String errorMsg = (String) failed.get("error_msg");
            
            BaseRecord record = recordMap.get(messageId);
            if (record != null) {
                System.err.println("Failed to create record for message: " + messageId + 
                                  ", error: " + errorCode + " - " + errorMsg);
                
                if (isRecoverableError(errorCode)) {
                    // 可恢复错误，重新加入队列
                    baseBatchService.addRecord(record);
                } else {
                    // 不可恢复错误，直接创建降级记录
                    createFallbackRecord(record);
                }
            }
        }
    }
    
    private boolean isRecoverableError(String errorCode) {
        // 40001 - 无效token
        // 40003 - 无权限
        // 40014 - 非法参数
        // 99999 - 系统错误（可重试）
        return !Set.of("40001", "40003", "40014").contains(errorCode);
    }
    
    private void createFallbackRecord(BaseRecord record) {
        // 创建降级记录
        System.out.println("Creating fallback record for: " + record.getMessageId());
    }
    
    @Recover
    public void recoverBaseWrite(FeishuApiException e, BaseRecord record) {
        System.err.println("Failed to write to Base after retries: " + record.getMessageId());
        createFallbackRecord(record);
    }
}