# API v1.4 Migration Project

> Complete enterprise API modernization covering application layer, database optimization, and cloud infrastructure deployment

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.2-brightgreen. svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Resilience4j](https://img.shields.io/badge/Resilience4j-2.0.2-blue. svg)](https://resilience4j.readme.io/)
[![Oracle](https://img.shields.io/badge/Oracle-Database-red.svg)](https://www.oracle.com/database/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Deployed-blue.svg)](https://kubernetes.io/)

**Author:** Vijay Soundaram  
**GitHub:** [@vijayyovan](https://github.com/vijayyovan)  
**Date:** December 2025

> 📌 **Portfolio Project:** This documentation represents production work completed as part of enterprise system development. 

---

## 🎯 Project Overview

Two-phase enterprise API migration from v1.3 to v1.4, implementing modern fault-tolerance patterns, HTTP response code standardization, and high-availability Kubernetes deployment.

### Business Context

The Enterprise Management API (EMA) provides real-time outage and event information to customer service applications (AOM, CARS, IRIS). This v1.4 migration addressed critical issues: 

- **Problem:** Ambiguous error responses prevented proper error handling
- **Solution:** Standardized HTTP codes with clear differentiation
- **Impact:** Improved system integration and customer experience

---

## 🏗️ Project Phases

### Phase 1: Application Layer Migration
**Focus:** Spring Boot, Resilience4j, Circuit Breaker Pattern  
**Skills:** Java 17, Fault Tolerance, RESTful API Design  
**Highlights:** Zero-downtime migration, Map-based contracts, 65% performance improvement

[📖 View Phase 1 Documentation →](./spring-boot-migration/)

### Phase 2: Database & Infrastructure
**Focus:** Oracle PL/SQL, Kubernetes, HTTP Standards Implementation  
**Skills:** Database Development, DevOps, Comprehensive Testing  
**Highlights:** Response code standardization, HA deployment, 100% test coverage

[📖 View Phase 2 Documentation →](./database-infrastructure/)

---

## 🏆 Key Achievements

| Achievement | Impact |
|-------------|--------|
| **Zero-downtime migration** | 3 production endpoints migrated without service interruption |
| **Response code standardization** | Eliminated ambiguous error responses (404 → 200/204/400) |
| **Fault-tolerant architecture** | Resilience4j circuit breakers with graceful degradation |
| **Performance optimization** | 65% improvement via Oracle result caching |
| **High availability deployment** | Kubernetes with 6-node load balancing |
| **Comprehensive testing** | 46+ test scenarios, 100% pass rate |

---

## 📊 Project Metrics

| Metric | Value |
|--------|-------|
| **Endpoints Migrated** | 3 production REST APIs |
| **Response Codes Handled** | 8 distinct codes (0, 1, 200, 202, 204, 400, 404, 503) |
| **Performance Gain** | 65% faster (cold:  9.5s → cached: 3.4s) |
| **Test Coverage** | 46+ scenarios across 6 event source types |
| **Deployment Model** | Kubernetes with 6-node HA configuration |
| **Availability** | Circuit breaker + load balancing + health probes |
| **Error Rate** | 0% post-implementation |

---

## 💼 Skills Demonstrated

### Backend Development
- ✅ **Spring Boot 3.x** - Advanced configuration, AOP, dependency injection
- ✅ **Java 17** - Modern Java features, stream API, functional programming
- ✅ **Oracle PL/SQL** - Stored procedures, result caching, performance tuning
- ✅ **RESTful API Design** - Versioning, contract evolution, HTTP semantics

### Resilience & Reliability
- ✅ **Circuit Breaker Pattern** - Resilience4j implementation with fallback strategies
- ✅ **Fault Tolerance** - Graceful degradation, error handling, retry logic
- ✅ **Logging & Monitoring** - GUID-based request tracking, structured logging

### DevOps & Infrastructure
- ✅ **Kubernetes** - Deployment configuration, pod management, replica sets
- ✅ **Rancher** - Container orchestration, cluster management
- ✅ **Load Balancing** - 6-node HA configuration, traffic distribution
- ✅ **Ingress Configuration** - 28 routes, multiple hostnames, path-based routing

### Testing & Quality Assurance
- ✅ **Test Scenario Design** - 46+ comprehensive test cases
- ✅ **API Testing** - Postman collections, cURL commands
- ✅ **Performance Benchmarking** - Response time analysis, optimization validation
- ✅ **Integration Testing** - End-to-end validation across systems

---

## 🚀 Quick Start

### Prerequisites
```bash
Java 17+
Maven 3.9+
Oracle Database 19c
Spring Boot 3.3.2
Kubernetes cluster (for deployment)
```

### API Endpoints

#### EventDetail v1.4
```bash
GET /net-ops/ema/event/v1.4/detail/{eventId}
Headers: 
  Session-ID: {sessionId}
  Transaction-ID: {transactionId}
  Client-ID: {clientId}
Responses:  200 OK | 404 Not Found | 400 Bad Request
```

#### OutageDetail v1.4
```bash
GET /net-ops/ema/outages/v1.4/detail/? accountNumber={acct}&divisionId={div}
Headers: Session-ID, Transaction-ID, Client-ID
Responses: 200 OK | 204 No Content | 400 Bad Request
```

#### OutageHistory v1.4
```bash
GET /net-ops/ema/outages/v1.4/history/? accountNumber={acct}&divisionId={div}
Headers: Session-ID, Transaction-ID, Client-ID
Responses: 200 OK | 204 No Content | 400 Bad Request
```

---

## 🔥 Key Technical Highlights

### 1. Circuit Breaker Implementation

```java
@CircuitBreaker(name = "eventDetailsService", fallbackMethod = "handleFallback")
public Map<String, Object> getEventDetailsContract(
        String guid, String eventId, String sessionId, 
        String transactionId, String clientId) {
    // Business logic with fault tolerance
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
    fallbackResponse. put("RETURN_CODE", 503);
    fallbackResponse. put("RESULTS_STRING", 
        "{\"error\": \"Service temporarily unavailable\"}");
    return fallbackResponse;
}
```

### 2. HTTP Response Code Standardization

**Problem Solved:** v1.3 returned same error for different scenarios

```java
// v1.3 - Ambiguous (Both returned 404)
Account doesn't exist → 404 "No Outages to Return"
Account exists, no outages → 404 "No Outages to Return"

// v1.4 - Clear Differentiation
switch (returnCode) {
    case 0:    // Event not found
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                             . body("{\"error\": \"No Record Found\"}");
    
    case 1:    // Success (Oracle SP convention)
    case 200:  // Success (HTTP-style)
        return ResponseEntity.ok().body(body);
    
    case 204:  // No content available
        return ResponseEntity.noContent().build();
    
    case 400:  // Bad request / invalid account
        return ResponseEntity.badRequest().body(body);
    
    default:   // Unexpected codes
        logger.error("Unexpected RETURN_CODE:  {}", returnCode);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body("{\"error\": \"Internal server error\"}");
}
```

### 3. Oracle Stored Procedure Integration

```java
// Map-based contract for structured responses
Map<String, Object> result = eventDetailsRepository.getEventDetailsContract(
    environment, guid, currentTimestamp, eventId, 
    sessionId, transactionId, clientId
);

Number rcNum = (Number) result.get("RETURN_CODE");
String body = (String) result.get("RESULTS_STRING");
String logging = (String) result.get("LOGGING");

int returnCode = (rcNum != null) ? rcNum.intValue() : -1;
```

---

## 📈 Performance Analysis

| Endpoint | Cold Start | Cached | Improvement |
|----------|-----------|--------|-------------|
| **EventDetail** | 606 ms | 606 ms | N/A (lightweight query) |
| **OutageDetail** | 9,567 ms | 3,359 ms | **65% faster** 🎯 |
| **OutageHistory** | 1,433 ms | ~800 ms | **44% faster** 🎯 |

**Optimization:** Oracle `RESULT_CACHE` hint significantly improves repeat query performance

```sql
SELECT /*+ RESULT_CACHE */ 
    event_id, outage_details, etr
FROM events
WHERE account_number = :accountNumber;
```

---

## 🐛 Technical Challenges Solved

### Challenge #1: Circuit Breaker Fallback Method Not Found
**Problem:** `NoSuchMethodException` at runtime  
**Root Cause:** Method signature mismatch (missing `Throwable` parameter)  
**Solution:** Aligned fallback method signature with main method + added `Throwable`

### Challenge #2: Unexpected RETURN_CODE 1
**Problem:** Oracle SP returns `1` for success, not HTTP `200`  
**Root Cause:** Oracle convention (0=failure, 1=success) vs HTTP semantics  
**Solution:** Added `case 1:` to map Oracle success to HTTP 200

### Challenge #3: HashMap Import Missing
**Problem:** Compilation error in fallback method  
**Root Cause:** Missing `java.util.HashMap` import statement  
**Solution:** Added proper import declaration

[**View All 7 Challenges & Solutions →**](./spring-boot-migration/TECHNICAL_CHALLENGES.md)

---

## 📁 Repository Structure

```
API-v1.4-Migration/
├── README.md                          # This file
├── COMBINED_OVERVIEW.md               # Architecture overview
├── spring-boot-migration/             # Phase 1: Application layer
│   ├── README.md
│   ├── TECHNICAL_CHALLENGES.md
│   ├── IMPLEMENTATION_GUIDE.md
│   ├── ARCHITECTURE.md
│   ├── code-samples/
│   │   ├── before-v13/
│   │   ├── after-v14/
│   │   └── fixes/
│   ├── docs/
│   │   ├── api-contract.md
│   │   ├── return-code-mapping.md
│   │   └── circuit-breaker-config.md
│   └── testing/
│       ├── test-scenarios.md
│       ├── curl-commands.md
│       └── test-results.md
└── database-infrastructure/           # Phase 2: Database & K8s
    ├── README.md
    ├── docs/
    │   ├── API_RESPONSE_CODES.md
    │   ├── STORED_PROCEDURE_CHANGES.md
    │   └── ISSUES_AND_RESOLUTIONS.md
    ├── database/
    │   └── procedures/
    │       ├── sp_outage_details_v1_4.sql
    │       ├── sp_outage_history_v1_4.sql
    │       └── sp_event_details_v1_4.sql
    ├── kubernetes/
    │   ├── deployment. yaml
    │   ├── service.yaml
    │   ├── ingress.yaml
    │   └── configmap.yaml
    └── tests/
        └── test-scenarios/
            ├── functional-tests.md
            ├── lh-hoc-events.md
            ├── cars-events.md
            ├── iris-events.md
            ├── snap-events.md
            ├── oi-events.md
            └── auto-events.md
```

---

## 📚 Documentation

### Application Layer (Phase 1)
- [Technical Challenges & Solutions](./spring-boot-migration/TECHNICAL_CHALLENGES.md)
- [Step-by-Step Implementation Guide](./spring-boot-migration/IMPLEMENTATION_GUIDE.md)
- [System Architecture & Design Patterns](./spring-boot-migration/ARCHITECTURE.md)
- [API Contract Comparison (v1.3 vs v1.4)](./spring-boot-migration/docs/api-contract.md)
- [Code Samples & Examples](./spring-boot-migration/code-samples/)

### Database & Infrastructure (Phase 2)
- [HTTP Response Code Standards](./database-infrastructure/docs/API_RESPONSE_CODES. md)
- [Stored Procedure Changes & Optimizations](./database-infrastructure/docs/STORED_PROCEDURE_CHANGES.md)
- [Issues Encountered & Resolutions](./database-infrastructure/docs/ISSUES_AND_RESOLUTIONS.md)
- [Kubernetes Deployment Configuration](./database-infrastructure/kubernetes/)
- [Comprehensive Test Scenarios (46+)](./database-infrastructure/tests/test-scenarios/)

---

## 🔍 Code Comparison

### Before (v1.3) - String-based Response
```java
public ResponseEntity<String> getEventDetailsById(
        String guid, String eventId, String sessionId,
        String transactionId, String clientId) {
    
    String result = eventDetailsService.getEventDetails(
        guid, eventId, sessionId, transactionId, clientId
    );
    
    // Ambiguous error handling
    if (result == null || result.contains("No Record Found")) {
        return new ResponseEntity<>(result, HttpStatus.NOT_FOUND);
    }
    
    return new ResponseEntity<>(result, HttpStatus.ACCEPTED);
}
```

### After (v1.4) - Map-based Contract with Explicit Codes
```java
public ResponseEntity<String> getEventDetailsById(
        String guid, String eventId, String sessionId,
        String transactionId, String clientId) {
    
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
        default:   return ResponseEntity.internalServerError()
                                       .body("{\"error\": \"Internal error\"}");
    }
}
```

**Key Improvements:**
- ✅ Structured response object (Map) vs string parsing
- ✅ Explicit return code handling vs ambiguous string checks
- ✅ Multiple response codes (200, 204, 400, 404) vs single 202/404
- ✅ Circuit breaker integration for fault tolerance

---

## 🧪 Testing

### Test Coverage

**Total Scenarios:** 46+  
**Event Source Types:** 6 (LH HOC, CARS, IRIS, SNAP, OI, Auto Events)  
**Pass Rate:** 100%

### Test Categories
- ✅ Success scenarios (data found → HTTP 200)
- ✅ No content scenarios (zero outages → HTTP 204)
- ✅ Not found scenarios (invalid account → HTTP 400)
- ✅ Error scenarios (circuit breaker fallback → HTTP 503)
- ✅ Performance benchmarking (response times)
- ✅ Header validation (required headers)

[**View Complete Test Results →**](./database-infrastructure/tests/test-scenarios/)

---

## 💡 Lessons Learned

### Technical Lessons
1. **Circuit Breaker Signatures:** Fallback methods must match main method signature + include `Throwable` parameter
2. **Oracle Conventions:** Oracle SPs use 0/1 return codes, not HTTP status codes (requires mapping layer)
3. **AOP Proxies:** Spring AOP requires exact method signature matching for interceptors
4. **Result Caching:** Oracle's `RESULT_CACHE` hint can provide 60%+ performance improvement
5. **Contract Evolution:** Map-based contracts are more maintainable than string parsing

### Process Lessons
6. **Testing First:** Comprehensive test scenarios catch edge cases early
7. **Backward Compatibility:** Maintain v1.3 during migration reduces risk
8. **Documentation:** Clear documentation accelerates team onboarding

[**Read Full Lessons Learned →**](./spring-boot-migration/lessons-learned.md)

---

## 🎯 Use Cases

This project demonstrates production-ready skills in: 

- **API Modernization** - Migrating legacy systems to modern architectures
- **Microservices** - Fault-tolerant, resilient service design
- **Enterprise Integration** - Oracle database, stored procedure integration
- **Cloud Infrastructure** - Kubernetes deployment, container orchestration
- **DevOps** - CI/CD readiness, infrastructure as code
- **Quality Engineering** - Comprehensive testing, performance optimization

---

## 🚀 Quick Links

- [📖 View Spring Boot Migration (Phase 1) →](./spring-boot-migration/)
- [📖 View Database & Infrastructure Work (Phase 2) →](./database-infrastructure/)
- [📊 View Combined System Architecture →](./COMBINED_OVERVIEW.md)
- [✅ View All Test Results →](./database-infrastructure/tests/)
- [🔧 View Technical Challenges Solved →](./spring-boot-migration/TECHNICAL_CHALLENGES. md)

---

## 👤 About This Project

This repository showcases real-world enterprise software engineering, demonstrating:

- **Production-Ready Code** - Battle-tested patterns, comprehensive error handling
- **Systematic Problem-Solving** - Documented challenges and solutions
- **Modern Architecture** - Microservices, fault tolerance, containerization
- **Full-Stack Capabilities** - Application layer, database, infrastructure

**Author:** Vijay Soundaram  
**GitHub:** [@vijayyovan](https://github.com/vijayyovan)  
**LinkedIn:** [Add your LinkedIn URL]

---

## 📄 License

This is a portfolio/demonstration project showcasing production work. Code samples are sanitized and genericized for public sharing.

---

## ⭐ If You Found This Interesting

If you find this project valuable or informative, please consider starring the repository! 

------

**Built with:** Spring Boot 3.3.2 | Java 17 | Resilience4j | Oracle Database | Kubernetes

**Status:** ✅ Complete | ✅ Tested | ✅ Production-Ready

**Built with:** Spring Boot 3.3.2 | Java 17 | Resilience4j | Oracle Database | Kubernetes

**Status:** ✅ Complete | ✅ Tested | ✅ Production-Ready
