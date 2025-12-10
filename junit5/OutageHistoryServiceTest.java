package com.company.api.service;

import static org. junit.jupiter.api.Assertions.*;
import static org.mockito. ArgumentMatchers.*;
import static org.mockito. Mockito.*;

import java. util.*;

import org.junit.jupiter.api.BeforeEach;
import org. junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito. Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.company.api.config.AppConfig;
import com. company.api.repository.OutageHistoryRepository;

/**
 * Comprehensive Service Layer Tests for OutageHistoryService
 * 
 * Author: Vijay Yovan
 * Date: December 2025
 * Portfolio Project: API v1.4 Migration
 * 
 * Test Coverage:
 * - Success scenarios (200 OK, 204 No Content)
 * - Error handling (400 Bad Request, 500/503 errors)
 * - Optional parameters (time range, pagination, filters)
 * - Configuration validation (environment, query output table)
 * - Edge cases (null responses, special characters, multiple records)
 * 
 * Testing Stack:
 * - JUnit 5
 * - Mockito with @ExtendWith
 * - @InjectMocks for service under test
 * - @Mock for dependencies
 * 
 * Key Patterns: 
 * - Unit testing with mocked dependencies
 * - History retrieval validation
 * - Parameter handling (time ranges, pagination)
 * - Helper methods for test data creation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OutageHistoryService - Business Logic Tests")
class OutageHistoryServiceTest {

    @Mock
    private OutageHistoryRepository outageHistoryRepository;

    @Mock
    private AppConfig appConfig;

    @InjectMocks
    private OutageHistoryService outageHistoryService;

    // Test constants - using generic sanitized data
    private static final String GUID = "test-guid-xyz789";
    private static final String VALID_ACCOUNT = "1234567890123456";
    private static final String INVALID_ACCOUNT = "9999999999999999";
    private static final String VALID_DIVISION = "DIV. 1234";
    private static final String ENVIRONMENT = "test";
    private static final String QUERY_OUTPUT_TABLE = "TEST_OUTPUT_TABLE";
    private static final String SESSION_ID = "session-test-123";
    private static final String TRANSACTION_ID = "txn-test-456";
    private static final String CLIENT_ID = "client-test-789";

    @BeforeEach
    void setUp() {
        when(appConfig.getEnvironment()).thenReturn(ENVIRONMENT);
        when(appConfig.getQueryOutputTableName()).thenReturn(QUERY_OUTPUT_TABLE);
    }

    // ========== SUCCESS SCENARIOS ==========

    @Test
    @DisplayName("✅ Should return 200 with history data when account has resolved outages")
    void testGetOutageHistoryRaw_WithResolvedOutages_Returns200() {
        // Given
        Map<String, Object> mockResponse = createSuccessResponse();
        when(outageHistoryRepository.getOutageHistoryRaw(
            eq(ENVIRONMENT), anyString(), any(), eq(VALID_ACCOUNT), eq(VALID_DIVISION),
            eq(SESSION_ID), eq(TRANSACTION_ID), eq(CLIENT_ID),
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
            eq(QUERY_OUTPUT_TABLE)
        )).thenReturn(mockResponse);

        // When
        Map<String, Object> result = outageHistoryService.getOutageHistoryRaw(
            GUID, VALID_ACCOUNT, VALID_DIVISION,
            SESSION_ID, TRANSACTION_ID, CLIENT_ID,
            null, null, null, null, null, null, null
        );

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals(200, result.get("RETURN_CODE"), "Return code should be 200");
        assertNotNull(result.get("RESULTS_STRING"), "Results string should not be null");
        
        String resultsString = result.get("RESULTS_STRING").toString();
        assertTrue(resultsString.contains("history") || resultsString.contains("eventId"),
                   "Results should contain history data");
        
        verify(outageHistoryRepository, times(1)).getOutageHistoryRaw(
            eq(ENVIRONMENT), anyString(), any(), eq(VALID_ACCOUNT), eq(VALID_DIVISION),
            eq(SESSION_ID), eq(TRANSACTION_ID), eq(CLIENT_ID),
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
            eq(QUERY_OUTPUT_TABLE)
        );
    }

    @Test
    @DisplayName("✅ Should return 204 when account exists but has no history")
    void testGetOutageHistoryRaw_NoHistory_Returns204() {
        // Given
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("RETURN_CODE", 204);
        mockResponse.put("RESULTS_STRING", null);
        
        when(outageHistoryRepository.getOutageHistoryRaw(
            eq(ENVIRONMENT), anyString(), any(), eq(VALID_ACCOUNT), eq(VALID_DIVISION),
            eq(SESSION_ID), eq(TRANSACTION_ID), eq(CLIENT_ID),
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
            eq(QUERY_OUTPUT_TABLE)
        )).thenReturn(mockResponse);

        // When
        Map<String, Object> result = outageHistoryService.getOutageHistoryRaw(
            GUID, VALID_ACCOUNT, VALID_DIVISION,
            SESSION_ID, TRANSACTION_ID, CLIENT_ID,
            null, null, null, null, null, null, null
        );

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals(204, result. get("RETURN_CODE"), "Return code should be 204");
        assertNull(result.get("RESULTS_STRING"), "Results string should be null for no content");
        
        verify(outageHistoryRepository, times(1)).getOutageHistoryRaw(
            eq(ENVIRONMENT), anyString(), any(), eq(VALID_ACCOUNT), eq(VALID_DIVISION),
            eq(SESSION_ID), eq(TRANSACTION_ID), eq(CLIENT_ID),
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
            eq(QUERY_OUTPUT_TABLE)
        );
    }

    @Test
    @DisplayName("✅ Should handle multiple history records correctly")
    void testGetOutageHistoryRaw_MultipleRecords_ReturnsAll() {
        // Given
        Map<String, Object> mockResponse = createSuccessResponseWithMultipleRecords();
        when(outageHistoryRepository.getOutageHistoryRaw(
            eq(ENVIRONMENT), anyString(), any(), eq(VALID_ACCOUNT), eq(VALID_DIVISION),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            eq(QUERY_OUTPUT_TABLE)
        )).thenReturn(mockResponse);

        // When
        Map<String, Object> result = outageHistoryService.getOutageHistoryRaw(
            GUID, VALID_ACCOUNT, VALID_DIVISION,
            SESSION_ID, TRANSACTION_ID, CLIENT_ID,
            null, null, null, null, null, null, null
        );

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals(200, result.get("RETURN_CODE"), "Return code should be 200");
        String resultsString = result.get("RESULTS_STRING").toString();
        assertNotNull(resultsString, "Results string should not be null");
        assertTrue(resultsString.length() > 100, "Results should contain multiple records");
    }

    // ========== ERROR SCENARIOS ==========

    @Test
    @DisplayName("❌ Should return 400 when account does not exist")
    void testGetOutageHistoryRaw_AccountNotExists_Returns400() {
        // Given
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("RETURN_CODE", 400);
        mockResponse.put("RESULTS_STRING", "{\"error\": \"Account does not exist\"}");
        
        when(outageHistoryRepository.getOutageHistoryRaw(
            eq(ENVIRONMENT), anyString(), any(), eq(INVALID_ACCOUNT), eq(VALID_DIVISION),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            eq(QUERY_OUTPUT_TABLE)
        )).thenReturn(mockResponse);

        // When
        Map<String, Object> result = outageHistoryService.getOutageHistoryRaw(
            GUID, INVALID_ACCOUNT, VALID_DIVISION,
            SESSION_ID, TRANSACTION_ID, CLIENT_ID,
            null, null, null, null, null, null, null
        );

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals(400, result.get("RETURN_CODE"), "Return code should be 400");
        assertTrue(result.get("RESULTS_STRING").toString().contains("not exist"),
                   "Error message should indicate account does not exist");
    }

    @Test
    @DisplayName("❌ Should throw exception when repository fails")
    void testGetOutageHistoryRaw_RepositoryException_ThrowsException() {
        // Given
        when(outageHistoryRepository.getOutageHistoryRaw(
            eq(ENVIRONMENT), anyString(), any(), eq(VALID_ACCOUNT), eq(VALID_DIVISION),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            eq(QUERY_OUTPUT_TABLE)
        )).thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            outageHistoryService.getOutageHistoryRaw(
                GUID, VALID_ACCOUNT, VALID_DIVISION,
                SESSION_ID, TRANSACTION_ID, CLIENT_ID,
                null, null, null, null, null, null, null
            );
        });
        
        assertEquals("Database connection failed", exception.getMessage(),
                     "Exception message should match");
        
        verify(outageHistoryRepository, times(1)).getOutageHistoryRaw(
            eq(ENVIRONMENT), anyString(), any(), eq(VALID_ACCOUNT), eq(VALID_DIVISION),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            eq(QUERY_OUTPUT_TABLE)
        );
    }

    @Test
    @DisplayName("❌ Should throw exception when database times out")
    void testGetOutageHistoryRaw_DatabaseTimeout_ThrowsException() {
        // Given
        when(outageHistoryRepository.getOutageHistoryRaw(
            eq(ENVIRONMENT), anyString(), any(), eq(VALID_ACCOUNT), eq(VALID_DIVISION),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            eq(QUERY_OUTPUT_TABLE)
        )).thenThrow(new RuntimeException("Connection timeout"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            outageHistoryService.getOutageHistoryRaw(
                GUID, VALID_ACCOUNT, VALID_DIVISION,
                SESSION_ID, TRANSACTION_ID, CLIENT_ID,
                null, null, null, null, null, null, null
            );
        });
        
        assertEquals("Connection timeout", exception.getMessage(),
                     "Exception message should match");
        
        verify(outageHistoryRepository, times(1)).getOutageHistoryRaw(
            eq(ENVIRONMENT), anyString(), any(), eq(VALID_ACCOUNT), eq(VALID_DIVISION),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            eq(QUERY_OUTPUT_TABLE)
        );
    }

    // ========== OPTIONAL PARAMETERS TESTS ==========

    @Test
    @DisplayName("✅ Should handle optional time range parameters")
    void testGetOutageHistoryRaw_WithTimeRange_Success() {
        // Given
        Map<String, Object> mockResponse = createSuccessResponse();
        String startTime = "2023-01-01T00:00:00Z";
        String endTime = "2023-12-31T23:59:59Z";
        
        when(outageHistoryRepository.getOutageHistoryRaw(
            eq(ENVIRONMENT), anyString(), any(), eq(VALID_ACCOUNT), eq(VALID_DIVISION),
            eq(SESSION_ID), eq(TRANSACTION_ID), eq(CLIENT_ID),
            eq(startTime), eq(endTime), any(), any(), any(), any(), any(),
            eq(QUERY_OUTPUT_TABLE)
        )).thenReturn(mockResponse);

        // When
        Map<String, Object> result = outageHistoryService.getOutageHistoryRaw(
            GUID, VALID_ACCOUNT, VALID_DIVISION,
            SESSION_ID, TRANSACTION_ID, CLIENT_ID,
            startTime, endTime, null, null, null, null, null
        );

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals(200, result.get("RETURN_CODE"), "Return code should be 200");
        
        verify(outageHistoryRepository, times(1)).getOutageHistoryRaw(
            eq(ENVIRONMENT), anyString(), any(), eq(VALID_ACCOUNT), eq(VALID_DIVISION),
            eq(SESSION_ID), eq(TRANSACTION_ID), eq(CLIENT_ID),
            eq(startTime), eq(endTime), any(), any(), any(), any(), any(),
            eq(QUERY_OUTPUT_TABLE)
        );
    }

    @Test
    @DisplayName("✅ Should handle optional pagination parameters")
    void testGetOutageHistoryRaw_WithPagination_Success() {
        // Given
        Map<String, Object> mockResponse = createSuccessResponse();
        Integer limit = 10;
        Integer offset = 0;
        
        when(outageHistoryRepository. getOutageHistoryRaw(
            eq(ENVIRONMENT), anyString(), any(), eq(VALID_ACCOUNT), eq(VALID_DIVISION),
            eq(SESSION_ID), eq(TRANSACTION_ID), eq(CLIENT_ID),
            any(), any(), any(), eq(limit), eq(offset), any(), any(),
            eq(QUERY_OUTPUT_TABLE)
        )).thenReturn(mockResponse);

        // When
        Map<String, Object> result = outageHistoryService.getOutageHistoryRaw(
            GUID, VALID_ACCOUNT, VALID_DIVISION,
            SESSION_ID, TRANSACTION_ID, CLIENT_ID,
            null, null, null, limit, offset, null, null
        );

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals(200, result.get("RETURN_CODE"), "Return code should be 200");
        
        verify(outageHistoryRepository, times(1)).getOutageHistoryRaw(
            eq(ENVIRONMENT), anyString(), any(), eq(VALID_ACCOUNT), eq(VALID_DIVISION),
            eq(SESSION_ID), eq(TRANSACTION_ID), eq(CLIENT_ID),
            any(), any(), any(), eq(limit), eq(offset), any(), any(),
            eq(QUERY_OUTPUT_TABLE)
        );
    }

    @Test
    @DisplayName("✅ Should handle all optional parameters together")
    void testGetOutageHistoryRaw_WithAllOptionalParams_Success() {
        // Given
        Map<String, Object> mockResponse = createSuccessResponse();
        String startTime = "2023-01-01T00:00:00Z";
        String endTime = "2023-12-31T23:59:59Z";
        String etdBlocking = "false";
        Integer limit = 20;
        Integer offset = 5;
        String processId = "PROC1,PROC2";
        String qualification = "QUAL1";
        
        when(outageHistoryRepository.getOutageHistoryRaw(
            eq(ENVIRONMENT), anyString(), any(), eq(VALID_ACCOUNT), eq(VALID_DIVISION),
            eq(SESSION_ID), eq(TRANSACTION_ID), eq(CLIENT_ID),
            eq(startTime), eq(endTime), eq(etdBlocking), eq(limit), eq(offset), 
            eq(processId), eq(qualification),
            eq(QUERY_OUTPUT_TABLE)
        )).thenReturn(mockResponse);

        // When
        Map<String, Object> result = outageHistoryService.getOutageHistoryRaw(
            GUID, VALID_ACCOUNT, VALID_DIVISION,
            SESSION_ID, TRANSACTION_ID, CLIENT_ID,
            startTime, endTime, etdBlocking, limit, offset, processId, qualification
        );

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals(200, result.get("RETURN_CODE"), "Return code should be 200");
        
        verify(outageHistoryRepository, times(1)).getOutageHistoryRaw(
            eq(ENVIRONMENT), anyString(), any(), eq(VALID_ACCOUNT), eq(VALID_DIVISION),
            eq(SESSION_ID), eq(TRANSACTION_ID), eq(CLIENT_ID),
            eq(startTime), eq(endTime), eq(etdBlocking), eq(limit), eq(offset),
            eq(processId), eq(qualification),
            eq(QUERY_OUTPUT_TABLE)
        );
    }

    // ========== CONFIGURATION TESTS ==========

    @Test
    @DisplayName("✅ Should use correct environment from AppConfig")
    void testGetOutageHistoryRaw_UsesCorrectEnvironment() {
        // Given
        Map<String, Object> mockResponse = createSuccessResponse();
        when(outageHistoryRepository.getOutageHistoryRaw(
            eq(ENVIRONMENT), anyString(), any(), any(), any(),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            eq(QUERY_OUTPUT_TABLE)
        )).thenReturn(mockResponse);

        // When
        outageHistoryService.getOutageHistoryRaw(
            GUID, VALID_ACCOUNT, VALID_DIVISION,
            SESSION_ID, TRANSACTION_ID, CLIENT_ID,
            null, null, null, null, null, null, null
        );

        // Then
        verify(appConfig, times(1)).getEnvironment();
        verify(appConfig, times(1)).getQueryOutputTableName();
    }

    // ========== EDGE CASES ==========

    @Test
    @DisplayName("✅ Should handle special characters in account number")
    void testGetOutageHistoryRaw_SpecialCharacters_Success() {
        // Given
        String specialAccount = "1234-5678-9012-3456";
        Map<String, Object> mockResponse = createSuccessResponse();
        when(outageHistoryRepository.getOutageHistoryRaw(
            eq(ENVIRONMENT), anyString(), any(), eq(specialAccount), eq(VALID_DIVISION),
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            eq(QUERY_OUTPUT_TABLE)
        )).thenReturn(mockResponse);

        // When
        Map<String, Object> result = outageHistoryService.getOutageHistoryRaw(
            GUID, specialAccount, VALID_DIVISION,
            SESSION_ID, TRANSACTION_ID, CLIENT_ID,
            null, null, null, null, null, null, null
        );

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals(200, result.get("RETURN_CODE"), "Return code should be 200");
    }

    // ========== HELPER METHODS ==========

    private Map<String, Object> createSuccessResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("RETURN_CODE", 200);
        response.put("RESULTS_STRING", 
            "{\"history\":[{\"eventId\":\"EVT000067890123\",\"resolvedAt\":\"2024-01-15T10:00:00Z\",\"status\":\"RESOLVED\"}]}");
        return response;
    }

    private Map<String, Object> createSuccessResponseWithMultipleRecords() {
        Map<String, Object> response = new HashMap<>();
        response.put("RETURN_CODE", 200);
        StringBuilder json = new StringBuilder("{\"history\":[");
        for (int i = 0; i < 10; i++) {
            if (i > 0) json.append(",");
            json.append("{\"eventId\":\"EVT00001111").append(i).append("\",\"resolvedAt\":\"2024-01-15T10:00:00Z\"}");
        }
        json. append("]}");
        response.put("RESULTS_STRING", json.toString());
        return response;
    }
}
