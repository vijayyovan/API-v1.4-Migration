# API v1.4 Migration Project

> Enterprise-grade REST API migration with fault tolerance, response code standardization, and Kubernetes deployment

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.2-brightgreen. svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Resilience4j](https://img.shields.io/badge/Resilience4j-2.0.2-blue.svg)](https://resilience4j.readme.io/)
[![Oracle](https://img.shields.io/badge/Oracle-Database-red.svg)](https://www.oracle.com/database/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Deployed-blue.svg)](https://kubernetes.io/)

**Author:** Vijay Soundaram  
**GitHub:** [@vijayyovan](https://github.com/vijayyovan)  
**Date:** December 2025

---

## 🎯 Project Overview

Enterprise API migration from v1.3 to v1.4, implementing: 
- ✅ Fault-tolerant architecture with Resilience4j circuit breakers
- ✅ HTTP response code standardization (200, 204, 400)
- ✅ Oracle stored procedure integration
- ✅ Kubernetes deployment with high availability

### Business Impact

Resolved critical error handling issues where identical responses (HTTP 404) were returned for different scenarios, preventing proper error differentiation in downstream systems.

---

## 🏆 Key Achievements

| Achievement | Metric |
|-------------|--------|
| **Endpoints Migrated** | 3 production REST APIs |
| **Response Codes** | 8 distinct codes (0, 1, 200, 202, 204, 400, 404, 503) |
| **Performance** | 65% improvement via Oracle caching |
| **Test Coverage** | 46+ scenarios, 100% pass rate |
| **Deployment** | Kubernetes with 6-node HA |
| **Availability** | Circuit breaker + load balancing |

---

## 💼 Skills Demonstrated

### Backend Development
- **Spring Boot 3.x** - Circuit breakers, AOP, dependency injection
- **Java 17** - Modern features, functional programming
- **Oracle PL/SQL** - Stored procedures, performance tuning
- **RESTful API** - Versioning, HTTP semantics

### Infrastructure & DevOps
- **Kubernetes** - Deployments, services, ingress
- **Rancher** - Container orchestration
- **Load Balancing** - 6-node HA configuration
- **High Availability** - Health probes, replica sets

### Testing & Quality
- **Comprehensive Testing** - 46+ test scenarios
- **API Testing** - Postman, cURL validation
- **Performance Testing** - Benchmarking, optimization
- **Integration Testing** - End-to-end validation

---

## 📚 Documentation

### Core Documentation
- [📖 **API Contract**](./API_CONTRACT.md) - v1.3 vs v1.4 comparison
- [🏗️ **Architecture**](./ARCHITECTURE.md) - System design and patterns
- [🚀 **Implementation Guide**](./IMPLEMENTATION_GUIDE.md) - Step-by-step migration
- [🐛 **Technical Challenges**](./TECHNICAL_CHALLENGES.md) - Problems solved
- [💡 **Lessons Learned**](./LESSONS_LEARNED.md) - Key takeaways
- [📋 **Deployment Challenges**](./DEPLOYMENT_CHALLENGES.md) - Kubernetes setup

### Testing Documentation
- [🧪 **Testing with JUnit5**](./Testing-Junit5) - Unit test implementation

---

## 🚀 API Endpoints

### EventDetail v1.4
```bash
GET /net-ops/ema/event/v1.4/detail/{eventId}
Headers: Session-ID, Transaction-ID, Client-ID
Responses:  200 OK | 404 Not Found | 400 Bad Request
```

### OutageDetail v1.4
```bash
GET /net-ops/ema/outages/v1.4/detail/? accountNumber={acct}&divisionId={div}
Headers: Session-ID, Transaction-ID, Client-ID
Responses: 200 OK | 204 No Content | 400 Bad Request
```

### OutageHistory v1.4
```bash
GET /net-ops/ema/outages/v1.4/history/?accountNumber={acct}&divisionId={div}
Headers: Session-ID, Transaction-ID, Client-ID
Responses: 200 OK | 204 No Content | 400 Bad Request
```

---

## 🔥 Technical Highlights

### 1. Circuit Breaker Pattern

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
    logger.error("Circuit breaker triggered", throwable);
    Map<String, Object> response = new HashMap<>();
    response.put("RETURN_CODE", 503);
    response.put("RESULTS_STRING", "{\"error\": \"Service unavailable\"}");
    return response;
}
```

### 2. Response Code Standardization

**Problem:** v1.3 returned identical errors for different scenarios

```java
// v1.3 - Ambiguous
Account doesn't exist → HTTP 404 "No Outages to Return"
Account exists, no outages → HTTP 404 "No Outages to Return"

// v1.4 - Clear Differentiation
switch (returnCode) {
    case 0:    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                   .body("{\"error\": \"No Record Found\"}");
    case 1:
    case 200: return ResponseEntity.ok().body(body);
    case 204: return ResponseEntity.noContent().build();
    case 400: return ResponseEntity.badRequest().body(body);
    default:  return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                   .body("{\"error\": \"Internal error\"}");
}
```

### 3. Oracle Stored Procedure Integration

```java
Map<String, Object> result = eventDetailsRepository.getEventDetailsContract(
    environment, guid, currentTimestamp, eventId, 
    sessionId, transactionId, clientId
);

Number rcNum = (Number) result.get("RETURN_CODE");
String body = (String) result.get("RESULTS_STRING");
int returnCode = (rcNum != null) ? rcNum.intValue() : -1;
```

---

## 📈 Performance Analysis

| Endpoint | Cold Start | Cached | Improvement |
|----------|-----------|--------|-------------|
| **EventDetail** | 606 ms | 606 ms | N/A |
| **OutageDetail** | 9,567 ms | 3,359 ms | **65% faster** 🎯 |
| **OutageHistory** | 1,433 ms | ~800 ms | **44% faster** 🎯 |

**Optimization:** Oracle `RESULT_CACHE` hint

---

## 🐛 Technical Challenges Solved

1. **Circuit Breaker Fallback Signature Mismatch**
   - Problem: `NoSuchMethodException` at runtime
   - Solution: Aligned method signatures, added `Throwable` parameter

2. **Oracle Return Code Convention**
   - Problem: SP returns `1` for success, not `200`
   - Solution: Added mapping layer for Oracle conventions

3. **HashMap Import Missing**
   - Problem: Compilation error in fallback method
   - Solution: Added `java.util.HashMap` import

[**View All Challenges →**](./TECHNICAL_CHALLENGES.md)

---

## 🔍 Code Comparison

### Before (v1.3)
```java
public ResponseEntity<String> getEventDetailsById(... ) {
    String result = eventDetailsService.getEventDetails(... );
    if (result == null || result.contains("No Record Found")) {
        return new ResponseEntity<>(result, HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(result, HttpStatus. ACCEPTED);
}
```

### After (v1.4)
```java
public ResponseEntity<String> getEventDetailsById(...) {
    Map<String, Object> result = eventDetailsService.getEventDetailsContract(... );
    int returnCode = ((Number) result.get("RETURN_CODE")).intValue();
    String body = (String) result.get("RESULTS_STRING");
    
    switch (returnCode) {
        case 0: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        case 1:
        case 200: return ResponseEntity.ok().body(body);
        case 204: return ResponseEntity.noContent().build();
        case 400: return ResponseEntity.badRequest().body(body);
        default: return ResponseEntity. internalServerError()
                                     .body("{\"error\": \"Internal error\"}");
    }
}
```

---

## 🧪 Testing

**Test Coverage:** 46+ scenarios  
**Pass Rate:** 100%

### Test Categories
- ✅ Success scenarios (HTTP 200)
- ✅ No content scenarios (HTTP 204)
- ✅ Error scenarios (HTTP 400, 404)
- ✅ Circuit breaker fallback (HTTP 503)
- ✅ Performance benchmarking
- ✅ Header validation

---

## 💡 Key Learnings

1. **Circuit Breaker Signatures** - Fallback methods need exact signature + `Throwable`
2. **Oracle Conventions** - SPs use 0/1, not HTTP codes
3. **AOP Proxies** - Spring requires precise method matching
4. **Result Caching** - `RESULT_CACHE` provides 60%+ improvement
5. **Contract Evolution** - Map-based > String parsing

[**Read Full Lessons →**](./LESSONS_LEARNED.md)

---

## 🎯 Use Cases

- **API Modernization** - Legacy to modern architecture
- **Microservices** - Fault-tolerant service design
- **Enterprise Integration** - Database integration
- **Cloud Infrastructure** - Kubernetes deployment
- **DevOps** - Infrastructure as code

---

## 📞 Contact

**Vijay Soundaram**  
**GitHub:** [@vijayyovan](https://github.com/vijayyovan)  
**LinkedIn:** [Add your LinkedIn]

---

## 📄 License

Portfolio/demonstration project.  Code samples are sanitized for public sharing. 

---

**Built with:** Spring Boot 3.3.2 | Java 17 | Resilience4j | Oracle Database | Kubernetes

**Status:** ✅ Complete | ✅ Tested | ✅ Production-Ready

---

⭐ **Star this repository if you found it helpful!**
