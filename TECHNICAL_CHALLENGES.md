# Technical Challenges & Solutions

> Detailed problem-solving journey for the EMA API v1.4 migration project

## Overview

This document chronicles the technical challenges encountered during the v1.3 → v1.4 migration, their root causes, solutions implemented, and key learnings.  Each challenge demonstrates real-world problem-solving skills and production engineering practices.

---

## Challenge #1: Circuit Breaker Fallback Method Not Found

### 🔴 Problem

```
java.lang.NoSuchMethodException: No qualifying method found for fallback
Caused by: java.lang.IllegalStateException: Expected fallback method 
'fallbackGetEventDetailsContract' for 'getEventDetailsContract' 
but could not find a method with matching signature. 
```

**Context:**  
Implementing Resilience4j circuit breaker pattern during v1.4 migration.  The main method was successfully proxied by Spring AOP, but the fallback method couldn't be located at runtime.

### 🔍 Root Cause Analysis

1. **Method Signature Mismatch:**
   - Main method:   `getEventDetailsContract(String, String, String, String, String)`
   - Fallback method:  `fallbackGetEventDetailsContract(String, String, String, String, String)`
   - **Missing:** `Throwable` parameter in fallback

2. **Annotation Reference:**
   ```java
   @CircuitBreaker(name = "eventDetailsService", fallbackMethod = "handleFallback")
   ```
   - Annotation references `"handleFallback"`
   - Actual method name was `fallbackGetEventDetailsContract`
   - **Name mismatch! **

3. **Spring AOP Proxy Behavior:**
   - Resilience4j uses AOP to intercept method calls
   - Fallback method must be discoverable at proxy creation time
   - Signature must exactly match:  same params + `Throwable`

### ✅ Solution Implemented

**Step 1: Fixed Method Name**
```java
// BEFORE (wrong name)
public Map<String, Object> fallbackGetEventDetailsContract(... ) {
    // ...
}

// AFTER (matches annotation)
public Map<String, Object> handleFallback(..., Throwable throwable) {
    // ...
}
```

**Step 2: Added Throwable Parameter**
```java
public Map<String, Object> handleFallback(
        String guid,
        String eventId,
        String sessionId,
        String transactionId,
        String clientId,
        Throwable throwable) {  // ← Added this
    
    logger.error("GUID {} - Circuit breaker fallback triggered for Event ID: {}", 
                 guid, eventId, throwable);
    
    Map<String, Object> fallbackResponse = new HashMap<>();
    fallbackResponse.put("RETURN_CODE", 503);
    fallbackResponse.put("RESULTS_STRING", "{\"error\": \"Service temporarily unavailable\"}");
    
    return fallbackResponse;
}
```

**Step 3: Verified Annotation**
```java
@CircuitBreaker(name = "eventDetailsService", fallbackMethod = "handleFallback")
//                                                              ^^^^^^^^^^^^^^
//                                                              Must match method name exactly
public Map<String, Object> getEventDetailsContract(... ) {
    // Business logic
}
```

### 📊 Impact

- ✅ Circuit breaker functional
- ✅ Graceful degradation on failures
- ✅ 503 Service Unavailable returned on fallback
- ✅ No more `NoSuchMethodException`

### 💡 Key Learnings

1. **Resilience4j Fallback Pattern:**
   ```java
   fallbackMethod(SameParamsAsMain.. ., Throwable throwable)
   ```

2. **Method Name Must Match:**
   - Annotation: `fallbackMethod = "handleFallback"`
   - Actual method: `public ...  handleFallback(... )`

3. **Spring AOP Considerations:**
   - Proxies require exact method signatures
   - Fallback must be in the same class
   - Return types must match

### 🎯 Skills Demonstrated

- Debugging Spring AOP proxy issues
- Understanding Resilience4j internals
- Method signature matching
- Circuit breaker pattern implementation
- Production error handling

---

## Challenge #2: Unexpected RETURN_CODE 1 from Stored Procedure

### 🔴 Problem

```
ERROR - Unexpected RETURN_CODE 1 from SP for Event ID: EVT000013552428 
(v1.4 contract violation)
Response: 500 Internal Server Error
```

**Context:**  
Controller switch statement only handled HTTP-style codes (200, 204, 400). Oracle stored procedure returned `1` for success, causing it to hit the `default` case and log as a contract violation.

**Logs showed:**
```
INFO - Stored Procedure executed with Return Code: 1, Client-ID: client2
INFO - Stored Procedure Logging: Event details retrieved successfully. 
ERROR - Unexpected RETURN_CODE 1 (v1.4 contract violation)
```

### 🔍 Root Cause Analysis

1. **Oracle SP Convention:**
   - Oracle stored procedures commonly use: 
     - `0` = Failure/Not Found
     - `1` = Success
     - `-1` = Special error conditions
   - This is **standard Oracle practice**, not HTTP status codes

2. **Controller Assumption:**
   - Switch statement only had cases for:  `200`, `204`, `400`
   - Assumed HTTP-style status codes
   - No case for Oracle-style `1`

3. **Contract Documentation Gap:**
   - v1.4 contract document showed future codes (`200`, `204`, `400`)
   - Didn't document **current** Oracle SP behavior
   - SP was returning `1`, not `200`

### ✅ Solution Implemented

**Added Case 1 Handler:**
```java
switch (returnCode) {
    case 0:
        // Event not found - Oracle SP convention
        logger.info("No event found for Event ID: {} (SP returned 0)", eventId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                             . headers(headers)
                             .body("{\"error\": \"No Record Found\"}");
    
    case 1:  // ← ADDED THIS
        // Event found - Oracle SP success code
        logger.info("Event found for Event ID: {} (SP returned 1)", eventId);
        return ResponseEntity.ok().headers(headers).body(body);
        
    case 200:
        // HTTP-style success (v1.4 contract - future)
        logger.info("Event found for Event ID: {} (SP returned 200)", eventId);
        return ResponseEntity.ok().headers(headers).body(body);
    
    // ... other cases
}
```

### 📊 Before vs After

**Before (Missing Case 1):**
```
SP returns 1 → hits default case → logs as violation → returns 500
```

**After (With Case 1):**
```
SP returns 1 → case 1 handler → logs success → returns 200 OK
```

### 🧪 Test Results

**Request:**
```bash
curl http://localhost:8191/net-ops/ema/event/v1.4/detail/EVT000013552428 \
  -H "Session-ID: sess2" -H "Transaction-ID: tran1" -H "Client-ID: client2"
```

**Before Fix:**
```
Status: 500 Internal Server Error
Body: {"error":  "Internal server error"}
Logs: ERROR - Unexpected RETURN_CODE 1 (contract violation)
```

**After Fix:**
```
Status: 200 OK
Body: {event JSON data}
Logs: INFO - Stored procedure returned code 1 (success)
```

### 💡 Key Learnings

1. **Oracle Conventions:**
   - Don't assume HTTP status codes from database
   - Oracle SPs often use `0/1` or similar conventions
   - Check actual SP output, not just documentation

2. **Contract Evolution:**
   - Current behavior vs future behavior
   - Document what's **actually happening** now
   - Plan for transition period

3. **Comprehensive Case Handling:**
   ```java
   case 0:   // Oracle:  not found
   case 1:   // Oracle:  success
   case 200: // HTTP: success (future)
   case 202: // Legacy success (transition)
   case 204: // No content
   case 400: // Bad request
   default:  // True violations
   ```

### 🎯 Skills Demonstrated

- Database integration patterns
- Understanding Oracle stored procedure conventions
- Contract analysis and documentation
- Backward compatibility handling
- Production debugging from logs

---

## Challenge #3: HashMap Import Missing in Service Class

### 🔴 Problem

```
Compilation error in EventDetailService.java:
Cannot resolve symbol 'HashMap'
```

**Context:**  
While implementing the circuit breaker fallback method, needed to create a `new HashMap<>()` for the fallback response. However, the import statement was missing.

### 🔍 Root Cause Analysis

1. **New Data Structure Usage:**
   - v1.3 used `String` return type (no Map needed)
   - v1.4 switched to `Map<String, Object>`
   - Fallback method creates new HashMap

2. **IDE Auto-Import Missed:**
   - Eclipse/IntelliJ didn't auto-import
   - Possibly because method was added manually
   - Common oversight when refactoring

### ✅ Solution Implemented

**Added Imports:**
```java
import java.util.HashMap;
import java.util.Map;
```

**Fallback Method Now Compiles:**
```java
public Map<String, Object> handleFallback(... , Throwable throwable) {
    Map<String, Object> fallbackResponse = new HashMap<>();  // ← Now works
    fallbackResponse.put("RETURN_CODE", 503);
    fallbackResponse.put("RESULTS_STRING", "{\"error\": \"Service temporarily unavailable\"}");
    return fallbackResponse;
}
```

### 💡 Key Learnings

1. **v1.3 → v1.4 Import Changes:**
   - v1.3: No Map imports needed (String-based)
   - v1.4: Need `HashMap` and `Map` imports

2. **Always Check Imports:**
   - When changing return types
   - When adding new data structures
   - Especially during refactoring

### 🎯 Skills Demonstrated

- Code refactoring
- Type system understanding
- Attention to compilation errors
- Systematic debugging

---

## Challenge #4: URL Encoding Issue (v1. 4 with Space)

### 🔴 Problem

```
WARN - No static resource outages/v1.%204/detail
Response: 404 Not Found
```

**Context:**  
Testing OutageDetail endpoint in Postman. URL had a space in `v1. 4` which got encoded as `v1.%204`, causing Spring to look for a static resource instead of routing to the controller.

### 🔍 Root Cause Analysis

1. **URL Encoding:**
   - User typed: `/outages/v1. 4/detail`
   - Browser/Postman encoded: `/outages/v1.%204/detail`
   - `%20` is URL encoding for space

2. **Spring Routing:**
   - Controller mapping: `@RequestMapping("/outages/v1.4/detail")`
   - Incoming request: `/outages/v1.%204/detail`
   - **No match** → Spring treats as static resource request

### ✅ Solution Implemented

**Fixed URL (removed space):**
```
BEFORE: /outages/v1. 4/detail? accountNumber=... 
AFTER:  /outages/v1.4/detail?accountNumber=... 
                 ^^^^
                 No space
```

### 💡 Key Learnings

1. **URL Syntax:**
   - No spaces allowed in URLs
   - Spaces get encoded as `%20`
   - Always validate URL format

2. **Testing Tools:**
   - Postman/curl don't always show encoding
   - Check browser network tab for actual request
   - Look for `%20` or `%2F` in URLs

### 🎯 Skills Demonstrated

- HTTP protocol understanding
- URL encoding knowledge
- Debugging web requests
- Testing tool proficiency

---

## Challenge #5: Performance - 9.5 Second Response Time

### 🔴 Problem

```
INFO - Stored Procedure executed with Return Code: 200, Elapsed Time: 9392 ms
INFO - Completed processing outage details, Elapsed Time: 9567 ms
```

**Context:**  
OutageDetail endpoint took 9.5 seconds to respond on first request. This was significantly slower than EventDetail (606ms).

### 🔍 Root Cause Analysis

**Analyzed SP Logging:**
```
Query 1: 528 ms   (Check no outage)
Query 2: 531 ms   (Check single outage)
Query 3: 528 ms   (Count by process ID)
Query 4: 7,782 ms (Get detailed outage data) ← BOTTLENECK
────────────────
Total:    9,369 ms
```

**Query 4 Details:**
- Complex joins:  `CCTT_CUSTOUTAGEEVENTJOIN_BLK2`
- Multiple `MIN()` aggregations
- Large result set
- `ORDER BY EVENTSTARTTIME DESC`

### ✅ Solution Discovered

**Oracle Result Cache Improved Performance:**

**Second Request (Cached):**
```
Query 1: 536 ms   (no change)
Query 2: 529 ms   (no change)
Query 3: 527 ms   (no change)
Query 4: 1,536 ms (was 7,782 ms - 80% faster!)
────────────────
Total:   3,128 ms (65% faster overall!)
```

**SP Uses Result Cache Hint:**
```sql
SELECT /*+ RESULT_CACHE */ ... 
```

### 📊 Performance Comparison

| Scenario | First Request | Cached Request | Improvement |
|----------|---------------|----------------|-------------|
| **OutageDetail** | 9,567 ms | 3,359 ms | **65% faster** |
| **Query 4 Only** | 7,782 ms | 1,536 ms | **80% faster** |

### 💡 Key Learnings

1. **Oracle Result Cache:**
   - `/*+ RESULT_CACHE */` hint caches query results
   - Significant performance improvement on repeat queries
   - Automatic cache invalidation on data changes

2. **Complex Query Performance:**
   - First execution: Full table scans
   - Subsequent:  Cached results
   - Production systems benefit from warm caches

3. **Performance Acceptance:**
   - 9.5s initial load acceptable for complex business rules
   - 3.4s cached response acceptable
   - Users typically query same accounts repeatedly

### 🎯 Skills Demonstrated

- Performance analysis
- Query optimization understanding
- Oracle database features
- Production performance expectations

---

## Challenge #6: RETURN_CODE 0 for Event Not Found

### 🔴 Problem

```
INFO - Stored Procedure executed with Return Code: 0
INFO - Stored Procedure Logging: No Event details retrieved for Event ID
ERROR - Unexpected RETURN_CODE 0 (v1.4 contract violation)
Response: 500 Internal Server Error
```

**Context:**  
Testing non-existent event ID. Oracle SP correctly returned `0` to indicate "not found", but controller had no handler for this code.

### 🔍 Root Cause Analysis

1. **Oracle Convention:**
   - `0` = Not found / No records
   - `1` = Success
   - Standard Oracle stored procedure pattern

2. **Controller Gap:**
   - Had handlers for:  `1`, `200`, `204`, `400`
   - No handler for `0`
   - Treated as unexpected/violation

3. **v1.3 Behavior:**
   - v1.3 checked string:  `result.contains("No Record Found")`
   - v1.4 needs explicit code handling

### ✅ Solution Implemented

**Added Case 0 Handler:**
```java
case 0:
    // Event not found - Oracle SP returns 0
    logger.info("No event found for Event ID: {} (SP returned 0)", eventId);
    
    String notFoundMsg = (body != null && ! body.isBlank()) 
        ? body 
        : "{\"error\": \"No Record Found\"}";
    
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                         .headers(headers)
                         .body(notFoundMsg);
```

### 🧪 Test Results

**Before Fix:**
```
Event ID: EVT000009592575 (non-existent)
SP returns 0 → default case → logs violation → 500 error
```

**After Fix:**
```
Event ID: EVT000009592575
SP returns 0 → case 0 handler → logs "no event found" → 404 Not Found
```

### 💡 Key Learnings

1. **Test Not Found Scenarios:**
   - Don't just test success cases
   - Test edge cases:  not found, invalid input, errors
   - Discovered issue through comprehensive testing

2. **Oracle Code Patterns:**
   ```
   0  = Not found / Empty result
   1  = Success
   -1 = Error (sometimes)
   ```

3. **Match v1.3 Behavior:**
   - v1.3 returned 404 for "No Record Found"
   - v1.4 should do the same (code 0 → 404)

### 🎯 Skills Demonstrated

- Comprehensive testing
- Edge case handling
- Backward compatibility
- Production behavior matching

---

## Challenge #7: Method Name Mismatch in Circuit Breaker

### 🔴 Problem (Earlier Version)

```
@CircuitBreaker(name = "eventDetailsService", fallbackMethod = "handleFallback")
public Map<String, Object> handleFallback(... ) {  // ← WRONG!  This is main method! 
    // Business logic here
}

public Map<String, Object> fallbackGetEventDetailsContract(...) {
    // Fallback logic here
}
```

**Context:**  
Accidentally named the **main method** as `handleFallback`, which matched the annotation's `fallbackMethod` attribute. This created circular reference confusion.

### 🔍 Root Cause Analysis

1. **Naming Confusion:**
   - Main method should be:  `getEventDetailsContract()`
   - Fallback should be: `handleFallback()`
   - Had them backwards! 

2. **Copy-Paste Error:**
   - Likely copied fallback method and renamed incorrectly
   - Didn't update main method name

### ✅ Solution Implemented

**Correct Naming:**
```java
@CircuitBreaker(name = "eventDetailsService", fallbackMethod = "handleFallback")
public Map<String, Object> getEventDetailsContract(...) {  // ← Main method
    // Business logic
}

public Map<String, Object> handleFallback(..., Throwable throwable) {  // ← Fallback
    // Fallback logic
}
```

### 💡 Key Learnings

1. **Naming Convention:**
   ```
   Main:      get{Resource}Contract()
   Fallback: handle{Resource}Fallback() or just handleFallback()
   ```

2. **Circuit Breaker Pattern:**
   ```java
   @CircuitBreaker(fallbackMethod = "XXXXX")
   public ReturnType businessMethod(...) { }
   
   public ReturnType XXXXX(..., Throwable t) { }
   ```

### 🎯 Skills Demonstrated

- Design pattern understanding
- Method naming conventions
- Code organization
- Attention to detail

---

## Summary of Challenges

| # | Challenge | Root Cause | Solution | Impact |
|---|-----------|------------|----------|--------|
| 1 | Circuit breaker fallback not found | Signature mismatch | Added `Throwable` param | Fault tolerance working |
| 2 | RETURN_CODE 1 unexpected | Oracle vs HTTP codes | Added `case 1:` | 200 OK responses |
| 3 | HashMap import missing | Refactoring oversight | Added import | Code compiles |
| 4 | URL encoding issue | Space in URL | Removed space | Routing works |
| 5 | 9.5s response time | Complex queries | Oracle caching | 65% improvement |
| 6 | RETURN_CODE 0 unexpected | Missing handler | Added `case 0:` | 404 responses |
| 7 | Method name mismatch | Naming confusion | Fixed names | Clear code structure |

