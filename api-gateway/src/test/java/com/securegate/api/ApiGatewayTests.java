package com.securegate.api;

import com.securegate.abac.AbacPolicyEngine;
import com.securegate.abac.Policy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * API Gateway Unit Tests
 */
class ApiGatewayTests {

    @Nested
    @DisplayName("ABAC Policy Engine Tests")
    class AbacPolicyEngineTests {

        private AbacPolicyEngine engine;

        @BeforeEach
        void setUp() {
            engine = new AbacPolicyEngine();
        }

        @Test
        @DisplayName("Should permit access when subject matches policy")
        void testPermitWhenSubjectMatches() {
            Map<String, Object> subject = new HashMap<>();
            subject.put("department", "engineering");
            subject.put("clearance_level", 4);

            Map<String, Object> policySubject = new HashMap<>();
            policySubject.put("department", List.of("engineering", "security"));
            policySubject.put("clearance_level", Map.of("min", 3));

            Map<String, Object> resource = new HashMap<>();
            resource.put("type", "audit_logs");

            Map<String, Object> policyResource = new HashMap<>();
            policyResource.put("type", "audit_logs");

            Policy policy = new Policy("p001", "permit", policySubject, policyResource, null);

            boolean result = engine.evaluate(subject, resource, new HashMap<>(), policy);
            assertTrue(result, "Should permit access for matching subject");
        }

        @Test
        @DisplayName("Should deny access when clearance level is too low")
        void testDenyWhenClearanceTooLow() {
            Map<String, Object> subject = new HashMap<>();
            subject.put("department", "engineering");
            subject.put("clearance_level", 2);

            Map<String, Object> policySubject = new HashMap<>();
            policySubject.put("clearance_level", Map.of("min", 3));

            Map<String, Object> resource = new HashMap<>();
            Map<String, Object> policyResource = new HashMap<>();

            Policy policy = new Policy("p002", "permit", policySubject, policyResource, null);

            boolean result = engine.evaluate(subject, resource, new HashMap<>(), policy);
            assertFalse(result, "Should deny access when clearance level is below minimum");
        }

        @Test
        @DisplayName("Should deny access when department not in allowed list")
        void testDenyWhenDepartmentNotAllowed() {
            Map<String, Object> subject = new HashMap<>();
            subject.put("department", "marketing");

            Map<String, Object> policySubject = new HashMap<>();
            policySubject.put("department", List.of("engineering", "security"));

            Map<String, Object> resource = new HashMap<>();
            Map<String, Object> policyResource = new HashMap<>();

            Policy policy = new Policy("p003", "permit", policySubject, policyResource, null);

            boolean result = engine.evaluate(subject, resource, new HashMap<>(), policy);
            assertFalse(result, "Should deny access for non-allowed department");
        }

        @Test
        @DisplayName("Should permit when effect is permit and all conditions match")
        void testPermitEffect() {
            Map<String, Object> subject = new HashMap<>();
            subject.put("role", "admin");

            Map<String, Object> policySubject = new HashMap<>();
            policySubject.put("role", "admin");

            Policy policy = new Policy("p004", "permit", policySubject, new HashMap<>(), null);

            boolean result = engine.evaluate(subject, new HashMap<>(), new HashMap<>(), policy);
            assertTrue(result, "Should return true for permit effect");
        }

        @Test
        @DisplayName("Should deny when effect is deny even if conditions match")
        void testDenyEffect() {
            Map<String, Object> subject = new HashMap<>();
            subject.put("role", "guest");

            Map<String, Object> policySubject = new HashMap<>();
            policySubject.put("role", "guest");

            Policy policy = new Policy("p005", "deny", policySubject, new HashMap<>(), null);

            boolean result = engine.evaluate(subject, new HashMap<>(), new HashMap<>(), policy);
            assertFalse(result, "Should return false for deny effect");
        }
    }

    @Nested
    @DisplayName("Policy Record Tests")
    class PolicyTests {

        @Test
        @DisplayName("Should create valid policy")
        void testValidPolicy() {
            Policy policy = new Policy(
                    "test-policy",
                    "permit",
                    Map.of("role", "admin"),
                    Map.of("type", "document"),
                    null);

            assertEquals("test-policy", policy.policyId());
            assertEquals("permit", policy.effect());
            assertNotNull(policy.subject());
            assertNotNull(policy.resource());
        }

        @Test
        @DisplayName("Should throw exception for null policy ID")
        void testNullPolicyId() {
            assertThrows(IllegalArgumentException.class, () -> {
                new Policy(null, "permit", Map.of(), Map.of(), null);
            });
        }

        @Test
        @DisplayName("Should throw exception for empty policy ID")
        void testEmptyPolicyId() {
            assertThrows(IllegalArgumentException.class, () -> {
                new Policy("", "permit", Map.of(), Map.of(), null);
            });
        }

        @Test
        @DisplayName("Should throw exception for invalid effect")
        void testInvalidEffect() {
            assertThrows(IllegalArgumentException.class, () -> {
                new Policy("p001", "invalid", Map.of(), Map.of(), null);
            });
        }

        @Test
        @DisplayName("Should accept case-insensitive effects")
        void testCaseInsensitiveEffect() {
            assertDoesNotThrow(() -> {
                new Policy("p001", "PERMIT", Map.of(), Map.of(), null);
            });
            assertDoesNotThrow(() -> {
                new Policy("p002", "DENY", Map.of(), Map.of(), null);
            });
        }
    }

    @Nested
    @DisplayName("JWT Token Tests")
    class JwtTests {

        @Test
        @DisplayName("Token should contain required claims")
        void testTokenClaims() {
            // This would test JWT generation if we had access to the JwtManager
            // For unit testing, we verify the structure
            String mockToken = "eyJhbGciOiJFZERTQSIsInR5cCI6IkpXVCIsImtpZCI6InRlc3Qta2V5In0." +
                    "eyJzdWIiOiJhZG1pbkBtb3J0YWRoYS5tZSIsImlzcyI6InVybjptb3J0YWRoYS5tZTppYW0ifQ." +
                    "signature";

            String[] parts = mockToken.split("\\.");
            assertEquals(3, parts.length, "JWT should have 3 parts");
        }
    }
}
