# Feishu AI Assistant

This project implements a complete workflow for:
1. Listening to group messages in Feishu
2. Asynchronous AI processing
3. Writing results to Feishu Base (multidimensional table)

## Architecture

- **EventController**: Receives Feishu event callbacks
- **EventService**: Processes message events
- **AIService**: Calls AI service asynchronously
- **BaseService**: Writes records to Feishu Base
- **BaseBatchService**: Optimizes writes using batch processing
- **LockService**: Manages distributed locks for message processing
- **CircuitBreaker**: Implements resilience patterns for AI service calls

## Features

1. **Message Listening**
   - Receives and verifies Feishu group messages
   - Handles URL verification challenge

2. **AI Processing**
   - Asynchronous processing with circuit breaker
   - Timeout handling and fallback mechanisms
   - Signature verification for AI callbacks

3. **Base Writing**
   - Batch processing for efficient writes
   - Retry mechanism for failed operations
   - Error classification and handling

4. **Reliability Features**
   - Distributed locking with Redis
   - Circuit breaker for AI service
   - Batch queue with overflow handling
   - Comprehensive health checks

## Prerequisites

- Java 17+
- Redis 6+
- Feishu developer account
- AI service endpoint

## Configuration

Edit `src/main/resources/application.yml` with your configuration:

```yaml
feishu:
  app-id: YOUR_APP_ID
  app-secret: YOUR_APP_SECRET
  verification-token: YOUR_VERIFICATION_TOKEN
  base:
    app-id: YOUR_BASE_APP_ID
    table-id: YOUR_TABLE_ID

ai:
  service-url: YOUR_AI_SERVICE_URL
  callback-url: YOUR_AI_CALLBACK_URL

redis:
  host: YOUR_REDIS_HOST
  port: YOUR_REDIS_PORT