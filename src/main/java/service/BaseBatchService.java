package com.example.feishuai.service;

import com.example.feishuai.model.BaseRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class BaseBatchService {
    
    private static final int MAX_BATCH_SIZE = 50;
    private static final int MAX_QUEUE_SIZE = 1000;
    
    private final BlockingQueue<BaseRecord> recordQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    @Autowired
    private BaseService baseService;
    
    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(this::processBatch, 1, 1, TimeUnit.SECONDS);
    }
    
    public void addRecord(BaseRecord record) {
        if (!recordQueue.offer(record)) {
            System.out.println("Batch queue full, falling back to direct write");
            baseService.createSingleRecord(record);
        }
    }
    
    private void processBatch() {
        if (recordQueue.isEmpty()) return;
        
        List<BaseRecord> batch = new ArrayList<>(MAX_BATCH_SIZE);
        recordQueue.drainTo(batch, MAX_BATCH_SIZE);
        
        if (!batch.isEmpty()) {
            try {
                baseService.batchCreateRecords(batch);
            } catch (Exception e) {
                System.err.println("Batch processing failed, retrying individually");
                e.printStackTrace();
                batch.forEach(baseService::createSingleRecord);
            }
        }
    }
    
    public Map<String, Object> getQueueStatus() {
        return Map.of(
            "size", recordQueue.size(),
            "remainingCapacity", recordQueue.remainingCapacity()
        );
    }
    
    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            // 处理剩余记录
            processBatch();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}