# Enterprise REST API Modernization
## Spring Boot & Kubernetes Reference Implementation

>
>
>
> This is a reference implementation showcasing enterprise software engineering patterns and best practices. It demonstrates architectural approaches, design patterns, and technical solutions I've applied in professional environments. All business logic, data, and system names have been sanitized and genericized for educational and portfolio purposes.

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.2-brightgreen. svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Resilience4j](https://img.shields.io/badge/Resilience4j-2.0.2-blue.svg)](https://resilience4j.readme.io/)
[![Oracle](https://img.shields.io/badge/Oracle-Database-red.svg)](https://www.oracle.com/database/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Deployed-blue.svg)](https://kubernetes.io/)

**Author:** Vijay Soundaram  
**GitHub:** [@vijayyovan](https://github.com/vijayyovan)  
**Date:** December 2025

---

# 🎯 Project Overview

A reference implementation demonstrating enterprise API modernization patterns, fault-tolerance architecture, HTTP response standardization, and cloud-native Kubernetes deployment strategies.

### Technical Context

This project showcases the migration approach for modernizing legacy REST APIs to resilient microservices architecture. It demonstrates solutions to common enterprise challenges including ambiguous error handling, performance optimization, and zero-downtime deployments.

**Technical Problem Demonstrated:**
- Legacy Pattern: Identical HTTP responses for different error conditions prevented proper error handling
- Modern Solution: Clear response code differentiation (400 vs 204) enabling intelligent downstream error handling

---
### Executive Summary
- Migrated 3 legacy REST APIs to a resilient Spring Boot 3.x architecture
- Standardized ambiguous HTTP responses into contract-driven HTTP semantics
- Implemented Resilience4j circuit breakers with graceful degradation
- Achieved up to 65% performance improvement using Oracle result caching
- Deployed with zero downtime on Kubernetes using rolling updates and HA
---

## 🏆 Key Achievements

| Achievement | Impact |
|-------------|--------|
| **Zero-downtime migration** | 3 production endpoints migrated without service interruption |
| **Response code standardization** | Eliminated ambiguous error responses |
| **Fault-tolerant architecture** | Resilience4j circuit breakers with graceful degradation |
| **Performance optimization** | 65% improvement via Oracle result caching |
| **High availability** | Kubernetes with 6-node load balancing |
| **Comprehensive testing** | 46+ test scenarios, 100% pass rate |

---

## 📊 Project Metrics

| Metric | Value |
|--------|-------|
| **Endpoints Migrated** | 3 production REST APIs |
| **Response Codes Handled** | 8 distinct codes (0, 1, 200, 202, 204, 400, 404, 503) |
| **Performance Gain** | 65% faster (9. 5s → 3.4s with caching) |
| **Test Coverage** | 46+ scenarios across 6 event types |
| **Deployment** | Kubernetes with 6-node HA |
| **Error Rate** | 0% post-implementation |

---

## 💼 Skills Demonstrated

### Backend Development
- ✅ **Spring Boot 3.x** - Circuit breakers, AOP, dependency injection
- ✅ **Java 17** - Modern features, stream API, functional programming
- ✅ **Oracle PL/SQL** - Stored procedures, performance tuning, result caching
- ✅ **RESTful API Design** - Versioning, HTTP semantics, contract evolution

### Resilience & Reliability
- ✅ **Circuit Breaker Pattern** - Resilience4j with fallback strategies
- ✅ **Fault Tolerance** - Graceful degradation, retry logic
- ✅ **Error Handling** - Comprehensive error responses, logging
- ✅ **Monitoring** - GUID-based request tracking

### DevOps & Infrastructure
- ✅ **Kubernetes** - Deployments, services, ingress configuration
- ✅ **Rancher** - Container orchestration, cluster management
- ✅ **Load Balancing** - 6-node HA configuration
- ✅ **High Availability** - Health probes, replica sets, rolling updates

### Testing & Quality
- ✅ **Test Design** - 46+ comprehensive scenarios
- ✅ **API Testing** - Postman collections, cURL validation
- ✅ **Performance Testing** - Benchmarking, optimization validation
- ✅ **Integration Testing** - End-to-end validation

### Monitoring & Reliability
- ✅ **Splunk** - Custom dashboards, SPL queries, real-time alerting
- ✅ **Observability** - Metrics, logging, distributed tracing
- ✅ **SLA Monitoring** - Response time tracking, error rate analysis
- ✅ **Incident Response** - Alert configuration, escalation policies

 Production reliability tracking through custom Splunk dashboards monitoring API health, error rates, and circuit breaker metrics.
---

## 📚 Documentation

### Core Documentation
- [📋 **API Contract**](./API_CONTRACT. md) -Legacy vs Modern API comparison
- [🏗️ **Architecture**](./ARCHITECTURE.md) - System design and patterns
- [🚀 **Implementation Guide**](./IMPLEMENTATION_GUIDE.md) - Step-by-step migration process
- [🐛 **Technical Challenges**](./TECHNICAL_CHALLENGES.md) - Problems solved with solutions
- [💡 **Lessons Learned**](./LESSONS_LEARNED.md) - Key takeaways and best practices
- [🔧 **Deployment Challenges**](./DEPLOYMENT_CHALLENGES.md) - Kubernetes deployment issues

### Testing
- [🧪 **JUnit5 Testing**](./Testing-Junit5) - Unit test implementation
- [📊 **Test Scenarios**](./testing/) - Comprehensive test cases *(coming soon)*

---

## 🚀 API Endpoints

### EventDetail API
```bash
GET /net-ops/ema/event/v1.4/detail/{eventId}

Headers: 
  Session-ID:  {sessionId}
  Transaction-ID:  {transactionId}
  Client-ID: {clientId}

Responses:
  200 OK - Event details found
  404 Not Found - Event does not exist
  400 Bad Request - Invalid parameters
  503 Service Unavailable - Circuit breaker open
```

### OutageDetail v1.4
```bash
GET /net-ops/ema/outages/v1.4/detail/?accountNumber={acct}&divisionId={div}

Headers:
  Session-ID: {sessionId}
  Transaction-ID: {transactionId}
  Client-ID: {clientId}

Responses:
  200 OK - Outage details found
  204 No Content - Account valid, no outages
  400 Bad Request - Invalid account
  503 Service Unavailable - Circuit breaker open
```

### OutageHistory v1.4
```bash
GET /net-ops/ema/outages/v1.4/history/?accountNumber={acct}&divisionId={div}

Headers:
  Session-ID: {sessionId}
  Transaction-ID: {transactionId}
  Client-ID:  {clientId}

Responses: 
  200 OK - Outage history found
  204 No Content - Account valid, no history
  400 Bad Request - Invalid account
  503 Service Unavailable - Circuit breaker open
```

---

## 🔥 Technical Highlights

### 1. Circuit Breaker Implementation

```java
@CircuitBreaker(name = "eventDetailsService", fallbackMethod = "handleFallback")
public Map<String, Object> getEventDetailsContract(
        String guid, String eventId, String sessionId, 
        String transactionId, String clientId) {
    
    return eventDetailsRepository.getEventDetailsContract(
        environment, guid, currentTimestamp, eventId,
        sessionId, transactionId, clientId
    );
}

public Map<String, Object> handleFallback(
        String guid, String eventId, String sessionId,
        String transactionId, String clientId, Throwable throwable) {
    
    logger.error("Circuit breaker triggered for event: {}", eventId, throwable);
    
    Map<String, Object> fallbackResponse = new HashMap<>();
    fallbackResponse.put("RETURN_CODE", 503);
    fallbackResponse.put("RESULTS_STRING", 
        "{\"error\": \"Service temporarily unavailable\"}");
    
    return fallbackResponse;
}
```

**Configuration:**
```properties
resilience4j.circuitbreaker.instances.eventDetailsService.slidingWindowSize=10
resilience4j.circuitbreaker. instances.eventDetailsService.minimumNumberOfCalls=5
resilience4j.circuitbreaker.instances.eventDetailsService.failureRateThreshold=50
resilience4j.circuitbreaker.instances.eventDetailsService. waitDurationInOpenState=10s
```

---

### 2. Response Code Standardization

**Problem Solved:**
```
v1.3 Behavior (Ambiguous):
├─ Account doesn't exist → HTTP 404 "No Outages to Return"
└─ Account exists, no outages → HTTP 404 "No Outages to Return"
   ❌ Same response for different scenarios! 

v1.4 Behavior (Clear):
├─ Account doesn't exist → HTTP 400 "Account does not exist"
├─ Account exists, no outages → HTTP 204 (empty body)
└─ Account exists, has outages → HTTP 200 (with data)
   ✅ Distinct responses enable proper error handling! 
```

**Implementation:**
```java
switch (returnCode) {
    case 0:    // Event not found (Oracle convention)
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                             .body("{\"error\": \"No Record Found\"}");
    
    case 1:    // Success (Oracle SP convention)
    case 200:  // Success (HTTP-style)
        return ResponseEntity.ok().body(body);
    
    case 204:  // No content available
        return ResponseEntity.noContent().build();
    
    case 400:  // Bad request / invalid account
        return ResponseEntity.badRequest().body(body);
    
    case 503:  // Circuit breaker fallback
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    
    default:   // Unexpected codes
        logger.error("Unexpected RETURN_CODE: {} - contract violation", returnCode);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body("{\"error\": \"Internal server error\"}");
}
```

---

### 3. Oracle Stored Procedure Integration

```java
// Map-based contract for structured responses
Map<String, Object> result = eventDetailsRepository.getEventDetailsContract(
    environment, guid, currentTimestamp, eventId, 
    sessionId, transactionId, clientId
);

// Extract return code and response body
Number rcNum = (Number) result.get("RETURN_CODE");
String body = (String) result.get("RESULTS_STRING");
String logging = (String) result.get("LOGGING");

int returnCode = (rcNum != null) ? rcNum.intValue() : -1;
```

**Oracle Convention Mapping:**
```
Oracle SP Returns:        Maps To:
├─ 0 (failure)       →   HTTP 404 Not Found
├─ 1 (success)       →   HTTP 200 OK
├─ 200 (HTTP-style)  →   HTTP 200 OK
├─ 204 (no content)  →   HTTP 204 No Content
├─ 400 (bad request) →   HTTP 400 Bad Request
└─ Other codes       →   HTTP 500 Internal Server Error
```


---

## 📊 Monitoring & Observability

### Splunk Integration

Production reliability tracking through custom Splunk dashboards monitoring API health, error rates, and circuit breaker metrics.

#### Real-Time Error Rate Dashboard

**SPL Query for Error Rate Tracking:**
```splunk
index=app_logs sourcetype=ema_api_v1_4 env=PROD
| stats count as total_requests, 
        count(eval(status_code>=500)) as server_errors, 
        count(eval(status_code>=400 AND status_code<500)) as client_errors 
| eval error_rate_percent = round((server_errors / total_requests) * 100, 2)
| where error_rate_percent > 1.0
| table _time, total_requests, server_errors, error_rate_percent
```

#### Circuit Breaker Health Monitoring

**SPL Query for Circuit Breaker State:**
```splunk
index=app_logs sourcetype=ema_api_v1_4 "Circuit breaker"
| rex field=_raw "Circuit breaker (?<cb_state>OPEN|CLOSED|HALF_OPEN)"
| stats count by cb_state, circuit_breaker_name
| eval alert_level = if(cb_state="OPEN", "CRITICAL", "OK")
```

#### API Response Time Percentiles

**SPL Query for Performance Tracking:**
```splunk
index=app_logs sourcetype=ema_api_v1_4 endpoint="/event/v1.4/detail/*"
| stats perc50(response_time_ms) as p50,
        perc95(response_time_ms) as p95,
        perc99(response_time_ms) as p99,
        avg(response_time_ms) as avg_time
| eval p95_breach = if(p95 > 1000, "WARNING", "OK")
```

### Key Metrics Tracked

| Metric | Threshold | Alert Level |
|--------|-----------|-------------|
| Error Rate | > 1% | Warning |
| Error Rate | > 5% | Critical |
| Circuit Breaker Open | Any occurrence | Critical |
| P95 Response Time | > 1000ms | Warning |
| P99 Response Time | > 3000ms | Critical |
| 5xx Errors | > 10/hour | Warning |

### Alerting Strategy
```splunk
# Alert: High Error Rate
index=app_logs sourcetype=ema_api_v1_4 env=PROD earliest=-5m
| stats count as total, count(eval(status_code>=500)) as errors
| eval error_rate = (errors/total)*100
| where error_rate > 5
| eval alert_message = "CRITICAL: Error rate " + error_rate + "% exceeds 5% threshold"
```

### Dashboard Panels

1. **Request Volume** - Real-time request counts by endpoint
2. **Status Code Distribution** - HTTP response code breakdown
3. **Error Trends** - Time-series error rate visualization
4. **Circuit Breaker Health** - State transitions and failure counts
5. **Response Time Heatmap** - Performance distribution by time of day
6. **Top Errors** - Most frequent error messages with counts

---

---

## 📈 Performance Analysis

| Endpoint | First Request (Cold) | Cached Request | Improvement |
|----------|---------------------|----------------|-------------|
| **EventDetail** | 606 ms | 606 ms | N/A (lightweight query) |
| **OutageDetail** | 9,567 ms | 3,359 ms | **65% faster** 🎯 |
| **OutageHistory** | 1,433 ms | ~800 ms | **44% faster** 🎯 |

**Optimization Technique:**
```sql
SELECT /*+ RESULT_CACHE */ 
    event_id, outage_details, etr, customers_impacted
FROM events
WHERE account_number = :accountNumber
  AND division_id = :divisionId;
```

Oracle's `RESULT_CACHE` hint dramatically improves repeat query performance for frequently-accessed data.

---

## 🐛 Technical Challenges Solved

### Challenge #1: Circuit Breaker Fallback Method Not Found
**Problem:** `NoSuchMethodException: handleFallback(String, String, String, String, String)`  
**Root Cause:** Method signature mismatch - fallback method missing `Throwable` parameter  
**Solution:** Added `Throwable throwable` parameter to match Resilience4j requirements

**Before:**
```java
public Map<String, Object> handleFallback(
        String guid, String eventId, String sessionId,
        String transactionId, String clientId) { ... }
```

**After:**
```java
public Map<String, Object> handleFallback(
        String guid, String eventId, String sessionId,
        String transactionId, String clientId, Throwable throwable) { ... }
```

---

### Challenge #2: Unexpected RETURN_CODE 1
**Problem:** Oracle SP returns code `1` for success, causing unexpected behavior  
**Root Cause:** Oracle convention (0=failure, 1=success) differs from HTTP status codes  
**Solution:** Added `case 1:` to map Oracle success to HTTP 200

---

### Challenge #3: HashMap Import Missing
**Problem:** Compilation error: `cannot find symbol:  class HashMap`  
**Root Cause:** Missing import statement in fallback method  
**Solution:** Added `import java.util.HashMap;`

---

[**View All 7 Challenges & Detailed Solutions →**](./TECHNICAL_CHALLENGES.md)

---

## 🔍 Code Comparison

### Before (v1.3) - String-based Response
```java
public ResponseEntity<String> getEventDetailsById(
        String guid, String eventId, String sessionId,
        String transactionId, String clientId) {
    
    // String-based response parsing
    String result = eventDetailsService.getEventDetails(
        guid, eventId, sessionId, transactionId, clientId
    );
    
    // Ambiguous error handling via string parsing
    if (result == null || result.contains("No Record Found")) {
        return new ResponseEntity<>(result, HttpStatus.NOT_FOUND);
    }
    
    return new ResponseEntity<>(result, HttpStatus.ACCEPTED);
}
```

**Issues with v1.3:**
- ❌ String parsing for error detection (fragile)
- ❌ Single HTTP 202 for all success cases
- ❌ Single HTTP 404 for all failure cases
- ❌ No fault tolerance

---

### After (v1.4) - Map-based Contract
```java
@CircuitBreaker(name = "eventDetailsService", fallbackMethod = "handleFallback")
public ResponseEntity<String> getEventDetailsById(
        String guid, String eventId, String sessionId,
        String transactionId, String clientId) {
    
    // Structured response with explicit return codes
    Map<String, Object> result = eventDetailsService.getEventDetailsContract(
        guid, eventId, sessionId, transactionId, clientId
    );
    
    int returnCode = ((Number) result.get("RETURN_CODE")).intValue();
    String body = (String) result.get("RESULTS_STRING");
    
    // Clear, explicit response code handling
    switch (returnCode) {
        case 0:   return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        case 1:
        case 200: return ResponseEntity.ok().body(body);
        case 204: return ResponseEntity.noContent().build();
        case 400: return ResponseEntity. badRequest().body(body);
        case 503: return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        default:  return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                      .body("{\"error\": \"Internal server error\"}");
    }
}
```

**Improvements in v1.4:**
- ✅ Structured response object (Map) - no string parsing
- ✅ Explicit return codes for each scenario
- ✅ Multiple HTTP status codes (200, 204, 400, 404, 503)
- ✅ Circuit breaker integration for fault tolerance
- ✅ Clear error differentiation

---

## 🧪 Testing

### Test Coverage

**Total Test Scenarios:** 46+  
**Event Source Types:** 6 (LH HOC, CARS, IRIS, SNAP, OI, Auto Events)  
**Pass Rate:** 100%

### Test Categories
| Category | Scenarios | Status |
|----------|-----------|--------|
| Success scenarios (HTTP 200) | 21 | ✅ Pass |
| No content scenarios (HTTP 204) | 10 | ✅ Pass |
| Error scenarios (HTTP 400) | 11 | ✅ Pass |
| Not found scenarios (HTTP 404) | 4 | ✅ Pass |
| Circuit breaker fallback | 3 | ✅ Pass |
| Performance benchmarking | 3 | ✅ Pass |
| Header validation | 4 | ✅ Pass |

### Sample Test Scenarios

**Scenario 1: Valid Account with Outages**
```
Request: GET /outages/v1.4/history/?accountNumber=123&divisionId=DIV001
Expected: HTTP 200 with outage data JSON
Result: ✅ PASS (606ms)
```

**Scenario 2: Valid Account, Zero Outages**
```
Request: GET /outages/v1.4/history/?accountNumber=999&divisionId=DIV001
Expected: HTTP 204 (empty body)
Result: ✅ PASS (287ms)
```

**Scenario 3: Invalid Account**
```
Request: GET /outages/v1.4/history/?accountNumber=INVALID&divisionId=DIV001
Expected: HTTP 400 {"error": "Account does not exist"}
Result: ✅ PASS (156ms)
```

---

## 💡 Lessons Learned

### Technical Lessons

1. **Circuit Breaker Signatures**
   - Fallback methods must match main method signature exactly
   - Must include `Throwable` parameter as last argument
   - Spring AOP requires precise method matching

2. **Oracle Conventions**
   - Oracle SPs often use 0/1 return codes (not HTTP codes)
   - Requires mapping layer between Oracle and HTTP semantics
   - Document conventions clearly for team understanding

3. **AOP Proxies**
   - Spring AOP requires exact method signature matching
   - Proxy interceptors fail silently with signature mismatches
   - Test AOP aspects thoroughly in integration tests

4. **Result Caching**
   - Oracle's `RESULT_CACHE` hint can provide 60-65% improvement
   - Especially effective for frequently-accessed reference data
   - Cache invalidation strategy is critical

5. **Contract Evolution**
   - Map-based contracts are more maintainable than string parsing
   - Structured responses enable better error handling
   - Version APIs explicitly to manage breaking changes

### Process Lessons

6. **Testing First**
   - Comprehensive test scenarios catch edge cases early
   - Performance testing reveals optimization opportunities
   - Integration tests validate end-to-end behavior

7. **Backward Compatibility**
   - Maintain v1.3 during migration period reduces risk
   - Co-existence period enables gradual consumer migration
   - Monitoring both versions helps identify issues

8. **Documentation**
   - Clear documentation accelerates team onboarding
   - Decision logs capture rationale for future reference
   - Code comments explain "why" not just "what"

[**Read Full Lessons Learned →**](./LESSONS_LEARNED.md)

---

## 🎯 Use Cases

This project demonstrates production-ready capabilities in:

- **API Modernization** - Migrating legacy systems to modern architectures
- **Microservices** - Fault-tolerant, resilient service design
- **Enterprise Integration** - Oracle database and stored procedure integration
- **Cloud Infrastructure** - Kubernetes deployment and orchestration
- **DevOps** - Infrastructure as code, CI/CD readiness
- **Quality Engineering** - Comprehensive testing, performance optimization
- **Production Engineering** - Error handling, logging, monitoring

---

## 📁 Repository Structure

```
API-v1.4-Migration/
├── README.md                          # This file
├── API_CONTRACT.md                    # v1.3 vs v1.4 contract comparison
├── ARCHITECTURE.md                    # System design and patterns
├── IMPLEMENTATION_GUIDE.md            # Step-by-step migration process
├── TECHNICAL_CHALLENGES.md            # Problems solved with solutions
├── DEPLOYMENT_CHALLENGES.md           # Kubernetes deployment issues
├── LESSONS_LEARNED.md                 # Key takeaways and best practices
├── LICENSE. md                         # License information
├── Dockerfile                         # Container image definition
├── Testing-Junit5                     # JUnit5 test implementation
├── junit5/                            # Unit test files
└── k8s/                              # Kubernetes configuration files
```

---

## 👤 About This Project

This repository showcases real-world enterprise software engineering, demonstrating:

- **Production-Ready Code** - Battle-tested patterns, comprehensive error handling
- **Systematic Problem-Solving** - Documented challenges with clear solutions
- **Modern Architecture** - Microservices, fault tolerance, containerization
- **Full-Stack Capabilities** - Application layer, database, infrastructure

### My Role

- API design and implementation
- Oracle PL/SQL stored procedure development
- Kubernetes deployment configuration
- Test scenario design and execution
- Technical documentation
- Code review and quality assurance

---

#---

---

## 📞 Seeking DevOps/Platform Engineering Roles/SRE

**Vijay Soundaram** | DevOps Engineer | CKA Certified

- 🐙 **GitHub:** [@vijayyovan](https://github.com/vijayyovan) - See my Kubernetes deployments & CI/CD work
- 💼 **LinkedIn:** [linkedin.com/in/vijaysoundaram](https://www.linkedin.com/in/vijaysoundaram/)
- ✉️ **Email:** [vijay6206@gmail.com](mailto:vijay6206@gmail.com)

### 🎯 Target Roles
**DevOps Engineer** • **Platform Engineer** • **Site Reliability Engineer (SRE)** • **Cloud Infrastructure Engineer**

### 🛠️ Key Expertise
☸️ Kubernetes  • 🔄 CI/CD (Jenkins, GitLab) • 🏗️ IaC (Terraform, Ansible) • 📊 Monitoring (Splunk, Prometheus) • ☁️ Cloud-Native Architecture • 🔧 Container Orchestration (Rancher, Docker)

**17+ years** building and maintaining enterprise-grade infrastructure at scale.

---


---

## 📄 License

This is a portfolio/demonstration project showcasing production work. Code samples are sanitized and genericized for public sharing.

See [LICENSE.md](./LICENSE.md) for details.

---

## 🙏 Acknowledgments

- Architecture and design decisions made in collaboration with cross-functional teams
- Deployment support from DevOps team
- Testing coordination with QA team
- Stakeholder feedback from product management

---

## ⭐ Star This Repository

If you found this project interesting or helpful, please consider starring the repository! 

---

**Built with:** Spring Boot 3.3.2 | Java 17 | Resilience4j | Oracle Database | Kubernetes

**Status:** ✅ Complete | ✅ Tested | ✅ Production-Ready

**Last Updated:** December 2025
