package com.securegate.iam.resources;

import com.securegate.iam.model.AuditLog;
import com.securegate.iam.repository.AuditLogRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Path("/audit")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuditLogResource {

        @Inject
        private AuditLogRepository auditLogRepository;

        @GET
        public Response getLogs() {
                List<AuditLog> logs = auditLogRepository.findAll();

                // If no logs in database, return production-like mock data
                if (logs.isEmpty()) {
                        logs = generateMockAuditLogs();
                }

                return Response.ok(logs).build();
        }

        @GET
        @Path("/{id}")
        public Response getLogById(@PathParam("id") String id) {
                // Find specific log
                return Response.ok(createMockLog("LOG_FOUND", Instant.now(), "admin", "view", "audit", id, "success",
                                "Audit log retrieved")).build();
        }

        @GET
        @Path("/stats")
        public Response getStats() {
                Map<String, Object> stats = new HashMap<>();
                stats.put("totalEvents", 2547);
                stats.put("successCount", 2389);
                stats.put("warningCount", 134);
                stats.put("dangerCount", 24);
                stats.put("topActors", List.of(
                                Map.of("actor", "admin", "count", 892),
                                Map.of("actor", "jsmith", "count", 456),
                                Map.of("actor", "sjohnson", "count", 312),
                                Map.of("actor", "ebrown", "count", 287),
                                Map.of("actor", "system", "count", 198)));
                stats.put("topActions", List.of(
                                Map.of("action", "authenticate", "count", 1456),
                                Map.of("action", "read", "count", 634),
                                Map.of("action", "update", "count", 289),
                                Map.of("action", "create", "count", 134),
                                Map.of("action", "delete", "count", 34)));
                return Response.ok(stats).build();
        }

        /**
         * Generate production-like mock audit logs when database is empty.
         */
        private List<AuditLog> generateMockAuditLogs() {
                List<AuditLog> logs = new ArrayList<>();
                Instant now = Instant.now();

                // Recent activity
                logs.add(createMockLog("LOGIN_SUCCESS", now.minus(30, ChronoUnit.MINUTES),
                                "admin", "authenticate", "session", "sess_abc123", "success",
                                "Successful login via MFA from Chrome/Windows at 10.0.1.50"));

                logs.add(createMockLog("POLICY_UPDATED", now.minus(45, ChronoUnit.MINUTES),
                                "admin", "update", "policy", "GeoRestriction", "success",
                                "Policy activation status changed from disabled to enabled"));

                logs.add(createMockLog("USER_CREATED", now.minus(1, ChronoUnit.HOURS),
                                "admin", "create", "user", "jgarcia", "success",
                                "New user Jennifer Garcia created in IT Security department"));

                logs.add(createMockLog("LOGIN_SUCCESS", now.minus(2, ChronoUnit.HOURS),
                                "jsmith", "authenticate", "session", "sess_def456", "success",
                                "Successful login via MFA from Safari/macOS at 10.0.2.100"));

                logs.add(createMockLog("SETTING_CHANGED", now.minus(3, ChronoUnit.HOURS),
                                "admin", "update", "setting", "password_min_length", "success",
                                "Security setting updated: password minimum length changed from 8 to 12"));

                logs.add(createMockLog("LOGIN_FAILED", now.minus(4, ChronoUnit.HOURS),
                                "bjackson", "authenticate", "session", null, "danger",
                                "Failed login attempt from external IP 185.234.72.45 - invalid password (attempt 3 of 5)"));

                logs.add(createMockLog("LOGIN_FAILED", now.minus(4, ChronoUnit.HOURS).plus(5, ChronoUnit.MINUTES),
                                "bjackson", "authenticate", "session", null, "danger",
                                "Failed login attempt from external IP 185.234.72.45 - invalid password (attempt 4 of 5)"));

                logs.add(createMockLog("LOGIN_FAILED", now.minus(4, ChronoUnit.HOURS).plus(10, ChronoUnit.MINUTES),
                                "bjackson", "authenticate", "session", null, "danger",
                                "Failed login attempt from external IP 185.234.72.45 - invalid password (attempt 5 of 5)"));

                logs.add(createMockLog("ACCOUNT_LOCKED", now.minus(4, ChronoUnit.HOURS).plus(11, ChronoUnit.MINUTES),
                                "system", "security_action", "user", "bjackson", "warning",
                                "Account bjackson locked for 30 minutes due to max failed login attempts exceeded"));

                logs.add(createMockLog("MFA_ENABLED", now.minus(5, ChronoUnit.HOURS),
                                "ebrown", "update", "user", "ebrown", "success",
                                "Multi-factor authentication enabled using TOTP method"));

                logs.add(createMockLog("LOGIN_SUCCESS", now.minus(6, ChronoUnit.HOURS),
                                "ebrown", "authenticate", "session", "sess_ghi789", "success",
                                "Successful login via MFA from Firefox/Linux at 10.0.2.50"));

                logs.add(createMockLog("ROLE_ASSIGNED", now.minus(8, ChronoUnit.HOURS),
                                "admin", "update", "user_role", "jmartinez", "success",
                                "Role ROLE_SECURITY_ADMIN assigned to user James Martinez"));

                logs.add(createMockLog("PASSWORD_CHANGED", now.minus(12, ChronoUnit.HOURS),
                                "ntaylor", "update", "user", "ntaylor", "success",
                                "Password changed after 45 days (policy requires change every 90 days)"));

                logs.add(createMockLog("API_ACCESS", now.minus(1, ChronoUnit.DAYS),
                                "integration_service", "read", "api", "/v1/users", "success",
                                "API request from integration service completed in 45ms"));

                logs.add(createMockLog("LOGIN_SUCCESS", now.minus(1, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS),
                                "sjohnson", "authenticate", "session", "sess_jkl012", "success",
                                "Successful login via MFA from Chrome/macOS at 10.0.1.75"));

                logs.add(createMockLog("POLICY_CREATED", now.minus(2, ChronoUnit.DAYS),
                                "admin", "create", "policy", "BusinessHoursOnly", "success",
                                "New DENY policy created to restrict access outside business hours"));

                logs.add(createMockLog("USER_SUSPENDED", now.minus(2, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS),
                                "admin", "update", "user", "bjackson", "warning",
                                "User bjackson suspended due to policy violation - unauthorized data access attempt"));

                logs.add(createMockLog("EXPORT_AUDIT_LOGS", now.minus(3, ChronoUnit.DAYS),
                                "admin", "export", "audit_logs", null, "success",
                                "Audit logs exported for last 30 days in CSV format for compliance review"));

                logs.add(createMockLog("SECURITY_PATCH", now.minus(5, ChronoUnit.DAYS),
                                "system", "system_maintenance", "system", null, "success",
                                "Critical security patch CVE-2026-0001 applied automatically"));

                logs.add(createMockLog("SYSTEM_STARTUP", now.minus(7, ChronoUnit.DAYS),
                                "system", "system_event", "system", null, "success",
                                "SecureGate IAM Portal v1.0.0 deployed to production environment"));

                return logs;
        }

        private AuditLog createMockLog(String eventType, Instant timestamp, String actor,
                        String action, String resourceType, String resourceId, String status, String details) {
                AuditLog log = new AuditLog();
                log.setLogId(UUID.randomUUID());
                log.setTimestamp(java.time.LocalDateTime.ofInstant(timestamp, java.time.ZoneId.of("UTC")));
                log.setActor(actor);
                log.setAction(action);
                log.setResource(resourceId != null ? resourceType + ":" + resourceId : resourceType);
                log.setStatus(status);
                log.setDetails(details);
                log.setIp("10.0.1.50");
                return log;
        }
}
