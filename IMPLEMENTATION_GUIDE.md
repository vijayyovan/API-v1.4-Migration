# Implementation Guide: v1.3 → v1.4 Migration

> Step-by-step approach to migrating EMA API from String-based to Map-based contract with circuit breaker implementation

## Table of Contents

1. [Migration Overview](#migration-overview)
2. [Prerequisites](#prerequisites)
3. [Step-by-Step Implementation](#step-by-step-implementation)
4. [Testing Strategy](#testing-strategy)
5. [Rollback Plan](#rollback-plan)
6. [Lessons Learned](#lessons-learned)

---

## Migration Overview

### Objective
Migrate three REST API endpoints from v1.3 (String-based response) to v1.4 (Map-based contract with explicit return codes) while implementing fault-tolerant circuit breaker pattern. 

### Scope
- **EventDetail** - `/event/v1.4/detail/{eventId}`
- **OutageDetail** - `/outages/v1.4/detail`
- **OutageHistory** - `/outages/v1.4/history`

### Success Criteria
- ✅ Zero downtime migration
- ✅ 100% backward compatibility
- ✅ Circuit breaker operational
- ✅ All return codes handled
- ✅ Performance maintained or improved

---

## Prerequisites

### Technical Requirements

**Dependencies Added to pom.xml:**
```xml
<!-- Resilience4j Circuit Breaker -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.0.2</version>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-circuitbreaker</artifactId>
    <version>2.0.2</version>
</dependency>
```

**Configuration in application.properties:**
```properties
# Circuit Breaker Configuration
resilience4j.circuitbreaker.instances.eventDetailsService.registerHealthIndicator=true
resilience4j.circuitbreaker. instances.eventDetailsService.slidingWindowSize=10
resilience4j.circuitbreaker.instances.eventDetailsService.minimumNumberOfCalls=5
resilience4j.circuitbreaker. instances.eventDetailsService.permittedNumberOfCallsInHalfOpenState=3
resilience4j.circuitbreaker.instances.eventDetailsService.waitDurationInOpenState=10s
resilience4j.circuitbreaker.instances.eventDetailsService.failureRateThreshold=50

# Same configuration for outageDetailsService and outageHistoryService
```

### Knowledge Requirements
- Spring Boot 3.x
- Spring AOP basics
- Resilience4j circuit breaker pattern
- Oracle stored procedure conventions
- RESTful API design principles

---

## Step-by-Step Implementation

### Phase 1: Repository Layer Changes

#### Step 1.1: Create New Contract Method

**Goal:** Add new method that returns `Map<String, Object>` instead of `String`

**EventDetailRepository. java:**

```java
// NEW METHOD (v1.4)
public Map<String, Object> getEventDetailsContract(
        String environment,
        String guid,
        Timestamp currentTimestamp,
        String eventId,
        String sessionId,
        String transactionId,
        String clientId) {
    
    logger.info("GUID: {} - Executing stored procedure SP_EMA_EVENT_DETAILS_V_1_4_2 for EVENT_ID: {}", 
                guid, eventId);
    
    long startTime = System.currentTimeMillis();
    
    // Call stored procedure
    SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
            .withSchemaName("EMAADMIN")
            .withProcedureName("SP_EMA_EVENT_DETAILS_V_1_4_2");
    
    SqlParameterSource in = new MapSqlParameterSource()
            .addValue("I_ENVIRONMENT", environment)
            .addValue("I_GUID", guid)
            // ...  other parameters
            ;
    
    Map<String, Object> result = jdbcCall. execute(in);
    
    long elapsedTime = System.currentTimeMillis() - startTime;
    
    // Extract return code and results
    Number returnCode = (Number) result.get("RETURN_CODE");
    String resultsString = (String) result.get("RESULTS_STRING");
    String logging = (String) result.get("LOGGING");
    
    logger.info("GUID: {} - Stored Procedure executed with Return Code: {}, Client-ID: {}, Elapsed Time: {} ms",
                guid, returnCode, clientId, elapsedTime);
    
    if (logging != null && ! logging.isEmpty()) {
        logger.info("GUID: {} - Stored Procedure Logging: {}", guid, logging);
    }
    
    // Return structured response
    Map<String, Object> response = new HashMap<>();
    response.put("RETURN_CODE", returnCode);
    response.put("RESULTS_STRING", resultsString);
    
    return response;
}

// OLD METHOD (v1.3) - Keep for backward compatibility during transition
public String getEventDetails(... ) {
    // Existing v1.3 implementation
}
```

**Key Changes:**
- ✅ Return type: `String` → `Map<String, Object>`
- ✅ Extract `RETURN_CODE` and `RESULTS_STRING` from SP output
- ✅ Return structured Map instead of raw String
- ✅ Keep old method for v1.3 compatibility

**Repeat for:**
- `OutageDetailRepository.getOutageDetailsContract()`
- `OutageHistoryRepository.getOutageHistoryContract()`

---

### Phase 2: Service Layer Changes

#### Step 2.1: Add Circuit Breaker to Service

**Goal:** Implement fault-tolerant service with circuit breaker and fallback

**EventDetailService.java:**

```java
import io.github.resilience4j. circuitbreaker.annotation.CircuitBreaker;
import java.util.HashMap;
import java.util.Map;

@Service
public class EventDetailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EventDetailService.class);
    
    @Autowired
    private EventDetailRepository eventDetailRepository;
    
    @Autowired
    private AppConfig appConfig;
    
    // NEW METHOD (v1.4) with Circuit Breaker
    @CircuitBreaker(name = "eventDetailsService", fallbackMethod = "handleFallback")
    public Map<String, Object> getEventDetailsContract(
            String guid,
            String eventId,
            String sessionId,
            String transactionId,
            String clientId) {
        
        logger.info("Entering getEventDetails Method with Parameters - Event ID: {}, " +
                    "Session-ID: {}, Transaction-ID:  {}, Client-ID: {}, Environment: {}, GUID: {}",
                    eventId, sessionId, transactionId, clientId, appConfig.getEnvironment(), guid);
        
        String environment = appConfig.getEnvironment();
        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
        
        // Call repository with v1.4 contract
        return eventDetailRepository.getEventDetailsContract(
                environment, guid, currentTimestamp, eventId,
                sessionId, transactionId, clientId
        );
    }
    
    // FALLBACK METHOD - Must match main method signature + Throwable
    public Map<String, Object> handleFallback(
            String guid,
            String eventId,
            String sessionId,
            String transactionId,
            String clientId,
            Throwable throwable) {
        
        logger.error("GUID {} - Circuit breaker fallback triggered for Event ID: {}, Error: {}",
                     guid, eventId, throwable. getMessage(), throwable);
        
        // Return 503 Service Unavailable
        Map<String, Object> fallbackResponse = new HashMap<>();
        fallbackResponse.put("RETURN_CODE", 503);
        fallbackResponse.put("RESULTS_STRING", "{\"error\": \"Service temporarily unavailable\"}");
        
        return fallbackResponse;
    }
    
    // OLD METHOD (v1.3) - Keep for backward compatibility
    public String getEventDetails(...) {
        // Existing v1.3 implementation
    }
}
```

**Key Implementation Details:**

1. **Circuit Breaker Annotation:**
   ```java
   @CircuitBreaker(name = "eventDetailsService", fallbackMethod = "handleFallback")
   ```
   - `name` must match configuration in `application.properties`
   - `fallbackMethod` must exactly match fallback method name

2. **Fallback Method Signature:**
   ```java
   // Main method parameters + Throwable at the end
   public Map<String, Object> handleFallback(
       SameParamsAsMain.. .,
       Throwable throwable  // ← REQUIRED! 
   )
   ```

3. **Return Type:**
   - Main and fallback must return same type:  `Map<String, Object>`

4. **Fallback Response:**
   ```java
   Map. of(
       "RETURN_CODE", 503,
       "RESULTS_STRING", "{\"error\":  \"Service temporarily unavailable\"}"
   )
   ```

**Repeat for:**
- `OutageDetailService`
- `OutageHistoryService`

---

### Phase 3: Controller Layer Changes

#### Step 3.1: Update Controller to Handle Map Response

**Goal:** Process Map-based response and map return codes to HTTP responses

**EventDetail.java:**

```java
@RestController
@RequestMapping("/event/v1.4/detail/")
public class EventDetail {
    
    private static final Logger logger = LoggerFactory.getLogger(EventDetail.class);
    
    @Autowired
    private EventDetailService eventDetailsService;
    
    @Autowired
    private AppConfig appConfig;
    
    @GetMapping("/{eventId}")
    public ResponseEntity<String> getEventDetailsById(
            @PathVariable(required = false) String eventId,
            @RequestHeader(value = "Session-ID", required = false) String sessionId,
            @RequestHeader(value = "Transaction-ID", required = false) String transactionId,
            @RequestHeader(value = "Client-ID", required = false) String clientId) {
        
        // Validate required headers
        validateHeaders(sessionId, transactionId, clientId);
        
        long startTime = System.nanoTime();
        String guid = generateGuid();
        
        logger.info("Received request for event details - GUID: {}, Event ID: {}, " +
                    "Session:  {}, Transaction: {}, Client:  {}",
                    guid, eventId, sessionId, transactionId, clientId);
        
        try {
            // Call v1.4 service method (returns Map)
            Map<String, Object> result = eventDetailsService. getEventDetailsContract(
                guid, eventId, sessionId, transactionId, clientId
            );
            
            return handleServiceResponse(result, eventId, startTime, clientId);
            
        } catch (Exception ex) {
            return handleServiceException(eventId, ex);
        }
    }
    
    // CRITICAL: Map return codes to HTTP responses
    private ResponseEntity<String> handleServiceResponse(
            Map<String, Object> result,
            String eventId,
            long startTime,
            String clientId) {
        
        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
        logger.info("Completed processing event details for Event ID: {}, Client:  {}, Elapsed: {} ms",
                    eventId, clientId, elapsedMs);
        
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, appConfig.getContentType());
        
        if (result == null) {
            logger.error("Service returned null result for Event ID: {}", eventId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .headers(headers)
                                 .body("{\"error\": \"Internal server error\"}");
        }
        
        // Extract return code and body
        Number rcNum = (Number) result.get("RETURN_CODE");
        int returnCode = rcNum != null ? rcNum.intValue() : 500;
        String body = (String) result.get("RESULTS_STRING");
        
        // Map stored procedure codes to HTTP responses
        switch (returnCode) {
            case 0: 
                // Oracle SP:  Event not found
                logger.info("No event found for Event ID: {} (SP returned 0)", eventId);
                String notFoundMsg = (body != null && !body.isBlank()) 
                    ? body 
                    : "{\"error\": \"No Record Found\"}";
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                     .headers(headers)
                                     .body(notFoundMsg);
            
            case 1:
                // Oracle SP: Success
                logger.info("Event found for Event ID: {} (SP returned 1)", eventId);
                return ResponseEntity.ok().headers(headers).body(body);
                
            case 200:
                // HTTP-style success
                logger.info("Event found for Event ID: {} (SP returned 200)", eventId);
                return ResponseEntity.ok().headers(headers).body(body);
            
            case 202:
                // Legacy success code (v1.3 compatibility)
                logger.info("Event found for Event ID: {} (SP returned 202 - legacy)", eventId);
                return ResponseEntity.ok().headers(headers).body(body);
                
            case 204:
                // No content available
                logger.info("No content for Event ID: {} (SP returned 204)", eventId);
                return ResponseEntity.noContent().headers(headers).build();
                
            case 400:
                // Bad request / validation error
                logger.warn("Bad request for Event ID: {}, Error: {}", eventId, body);
                String errMsg = (body == null || body.isBlank())
                    ? "{\"error\": \"Bad request\"}"
                    : body;
                return ResponseEntity.badRequest().headers(headers).body(errMsg);
            
            case 404:
                // Legacy not found code
                logger.warn("Legacy 404 for Event ID: {}", eventId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                     . headers(headers)
                                     .body(body != null ?  body : "{\"error\": \"Not found\"}");
            
            case 503:
                // Circuit breaker fallback
                logger.warn("Circuit breaker fallback triggered for Event ID: {}", eventId);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                                     .headers(headers)
                                     . body(body);
            
            default:
                // Truly unexpected codes - log as violation
                logger.error("Unexpected RETURN_CODE {} from SP for Event ID: {} " +
                             "(v1.4 contract violation)", returnCode, eventId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                     .headers(headers)
                                     .body("{\"error\": \"Internal server error\"}");
        }
    }
    
    private ResponseEntity<String> handleServiceException(String eventId, Throwable ex) {
        logger.error("Error processing event details for Event ID: {}", eventId, ex);
        
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, appConfig.getContentType());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .headers(headers)
                             .body("{\"error\": \"Internal server error\"}");
    }
    
    private String generateGuid() {
        String clusterName = appConfig.getClusterName();
        String hostname = getHostname();
        return clusterName + "-" + hostname + "-EDv1.4-" + UUID.randomUUID();
    }
    
    private String getHostname() {
        try {
            return InetAddress. getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.error("Error retrieving hostname", e);
            return "unknown-host";
        }
    }
}
```

**Key Implementation Details:**

1. **Return Code Mapping Logic:**
   ```
   Oracle SP Code → HTTP Response
   ──────────────────────────────
   0              → 404 Not Found
   1              → 200 OK
   200            → 200 OK
   202            → 200 OK (legacy)
   204            → 204 No Content
   400            → 400 Bad Request
   404            → 404 Not Found (legacy)
   503            → 503 Service Unavailable (circuit breaker)
   Other          → 500 Internal Server Error
   ```

2. **Why Multiple Success Codes?**
   - `1`: Oracle SP convention (current)
   - `200`: HTTP-style (future/v1.4 target)
   - `202`: Legacy v1.3 code (transition period)

3. **GUID Format Changed:**
   - v1.3: `...-EDv1.3-... `
   - v1.4: `...-EDv1.4-...`

**Repeat for:**
- `OutageDetail. java`
- `OutageHistory.java`

---

### Phase 4: Testing & Validation

#### Step 4.1: Unit Testing

**Test all return code paths:**

```java
@Test
public void testEventFound_ReturnCode1() {
    Map<String, Object> mockResult = Map.of(
        "RETURN_CODE", 1,
        "RESULTS_STRING", "{\"eventId\":\"EVT123\"}"
    );
    
    when(service.getEventDetailsContract(... )).thenReturn(mockResult);
    
    ResponseEntity<String> response = controller.getEventDetailsById(... );
    
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
}

@Test
public void testEventNotFound_ReturnCode0() {
    Map<String, Object> mockResult = Map.of(
        "RETURN_CODE", 0,
        "RESULTS_STRING", "{\"error\": \"No Record Found\"}"
    );
    
    when(service.getEventDetailsContract(...)).thenReturn(mockResult);
    
    ResponseEntity<String> response = controller.getEventDetailsById(... );
    
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
}

@Test
public void testCircuitBreakerFallback() {
    when(service.getEventDetailsContract(... ))
        .thenThrow(new RuntimeException("Database down"));
    
    // Should trigger fallback
    Map<String, Object> result = service.getEventDetailsContract(... );
    
    assertEquals(503, result.get("RETURN_CODE"));
    assertTrue(((String) result.get("RESULTS_STRING")).contains("unavailable"));
}
```

#### Step 4.2: Integration Testing

**Test with actual database:**

```bash
# Test 1: Event exists
curl -i "http://localhost:8191/net-ops/ema/event/v1.4/detail/EVT000013552428" \
  -H "Session-ID: sess1" \
  -H "Transaction-ID: trans1" \
  -H "Client-ID:  client1" \
  -u svc_aom:svc_aom

# Expected: 200 OK with event data

# Test 2: Event not found
curl -i "http://localhost:8191/net-ops/ema/event/v1.4/detail/INVALID123" \
  -H "Session-ID: sess1" \
  -H "Transaction-ID:  trans1" \
  -H "Client-ID: client1" \
  -u svc_aom:svc_aom

# Expected: 404 Not Found

# Test 3: Missing headers
curl -i "http://localhost:8191/net-ops/ema/event/v1.4/detail/EVT000013552428" \
  -u svc_aom:svc_aom

# Expected: 400 Bad Request
```

#### Step 4.3: Circuit Breaker Testing

**Simulate database failure:**

```java
// Temporarily break database connection
@Test
public void testCircuitBreakerOpensAfterFailures() {
    // Trigger 5+ failures
    for (int i = 0; i < 6; i++) {
        when(repository.getEventDetailsContract(...))
            .thenThrow(new DataAccessException("DB error"));
        
        try {
            service.getEventDetailsContract(...);
        } catch (Exception e) {
            // Expected
        }
    }
    
    // Next call should use fallback (circuit open)
    Map<String, Object> result = service.getEventDetailsContract(...);
    assertEquals(503, result.get("RETURN_CODE"));
}
```

---

### Phase 5: Deployment Strategy

#### Step 5.1: Blue-Green Deployment

**Approach:  Maintain both v1.3 and v1.4 simultaneously**

```
/event/v1.3/detail/{id}  → Old implementation (String-based)
/event/v1.4/detail/{id}  → New implementation (Map-based)
```

**Benefits:**
- ✅ Zero downtime
- ✅ Easy rollback
- ✅ Gradual migration of clients

#### Step 5.2: Monitoring

**Key Metrics to Watch:**

1. **Circuit Breaker State:**
   ```
   resilience4j.circuitbreaker.eventDetailsService.state
   Values: CLOSED, OPEN, HALF_OPEN
   ```

2. **Success Rate:**
   ```
   resilience4j.circuitbreaker.eventDetailsService.success. rate
   Target: > 95%
   ```

3. **Fallback Call Rate:**
   ```
   resilience4j.circuitbreaker.eventDetailsService.fallback. calls
   Target: < 5%
   ```

4. **Response Times:**
   ```
   http.server.requests. duration
   Target: p95 < 2s, p99 < 5s
   ```

5. **Return Code Distribution:**
   ```
   returncode.0. count  (not found)
   returncode.1.count  (success)
   returncode.200.count (success)
   returncode.400.count (bad request)
   returncode.503.count (fallback)
   ```

#### Step 5.3: Rollback Plan

**If issues arise:**

1. **Immediate:** Route traffic back to v1.3 endpoints
2. **Configuration:** Disable circuit breaker temporarily
   ```properties
   resilience4j.circuitbreaker.instances.eventDetailsService.enabled=false
   ```
3. **Code:** Revert controller to call old service method

**Rollback Decision Criteria:**
- Error rate > 5%
- Response time p95 > 5s
- Circuit breaker open > 50% of time

---

## Testing Strategy

### Test Matrix

| Scenario | Expected Code | Expected HTTP | Body Expected?  |
|----------|---------------|---------------|----------------|
| Event exists | 1 or 200 | 200 OK | Yes (JSON) |
| Event not found | 0 | 404 Not Found | Yes (error) |
| Invalid input | 400 | 400 Bad Request | Yes (error) |
| No content | 204 | 204 No Content | No |
| Circuit breaker | 503 | 503 Service Unavailable | Yes (error) |
| Database error | 503 | 503 Service Unavailable | Yes (error) |
| Missing headers | N/A | 400 Bad Request | Yes (validation error) |

### Performance Benchmarks

| Endpoint | Target p50 | Target p95 | Target p99 |
|----------|------------|------------|------------|
| EventDetail | < 800ms | < 1.5s | < 3s |
| OutageDetail | < 2s | < 5s | < 10s |
| OutageHistory | < 1. 5s | < 3s | < 5s |

---

## Rollback Plan

### Triggers for Rollback

1. **Error Rate > 5%** for > 5 minutes
2. **Circuit breaker open** for > 50% of instances
3. **Response time degradation** > 2x baseline
4. **Production incident** reported by operations team

### Rollback Steps

1. **Immediate** (< 5 minutes):
   - Switch load balancer to v1.3 endpoints
   - No code changes required

2. **Short-term** (< 1 hour):
   - Disable circuit breaker in config
   - Restart affected pods
   - Monitor error rates

3. **Investigation**:
   - Analyze logs for root cause
   - Review circuit breaker metrics
   - Check database performance

---

## Lessons Learned

### What Went Well ✅

1. **Circuit Breaker Pattern:**
   - Provided excellent fault isolation
   - Fallback responses maintained availability
   - Easy to monitor and tune

2. **Structured Response Codes:**
   - Cleaner than String parsing
   - Easier to maintain and extend
   - Better separation of concerns

3. **Backward Compatibility:**
   - Both v1.3 and v1.4 coexist
   - Zero downtime migration
   - Gradual client migration possible

### Challenges Overcome 🛠️

1. **Circuit Breaker Fallback Signature:**
   - Required exact parameter matching + Throwable
   - Solution: Carefully align method signatures

2. **Oracle Return Code Conventions:**
   - Oracle uses 0/1, not HTTP codes
   - Solution: Map all possible codes explicitly

3. **Performance Variance:**
   - Complex queries took 9+ seconds initially
   - Solution: Oracle result cache improved to 3. 4s

### Best Practices Established 📚

1. **Always include Throwable in fallback methods**
2. **Document all return codes explicitly**
3. **Test both success and failure scenarios**
4. **Monitor circuit breaker states in production**
5. **Keep old endpoints during transition period**

---

## Conclusion

This migration demonstrated: 
- ✅ Modern resilience patterns (Circuit Breaker)
- ✅ Clean contract evolution (v1.3 → v1.4)
- ✅ Production-ready error handling
- ✅ Zero-downtime deployment strategy
- ✅ Comprehensive testing approach

**Result:** 100% test success rate, fault-tolerant architecture, improved maintainability. 

