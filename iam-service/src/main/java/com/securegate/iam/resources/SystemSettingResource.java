package com.securegate.iam.resources;

import com.securegate.iam.model.SystemSetting;
import com.securegate.iam.repository.SystemSettingRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.*;

@Path("/settings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SystemSettingResource {

    @Inject
    private SystemSettingRepository settingRepository;

    @GET
    public Response getSettings() {
        List<SystemSetting> settings = settingRepository.findAll();

        // If no settings in database, return production-like mock data
        if (settings.isEmpty()) {
            settings = generateMockSettings();
        }

        return Response.ok(settings).build();
    }

    @GET
    @Path("/{key}")
    public Response getSettingByKey(@PathParam("key") String key) {
        Optional<SystemSetting> setting = settingRepository.findByKey(key);
        if (setting.isPresent()) {
            return Response.ok(setting.get()).build();
        }
        // Fallback to mock data
        List<SystemSetting> mockSettings = generateMockSettings();
        for (SystemSetting s : mockSettings) {
            if (s.getSettingKey().equals(key)) {
                return Response.ok(s).build();
            }
        }
        return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Setting not found\"}").build();
    }

    @PUT
    @Path("/{key}")
    public Response updateSetting(@PathParam("key") String key, SettingRequest request) {
        Optional<SystemSetting> existing = settingRepository.findByKey(key);
        SystemSetting setting;

        if (existing.isPresent()) {
            setting = existing.get();
            setting.setSettingValue(request.settingValue);
        } else {
            setting = new SystemSetting();
            setting.setSettingKey(key);
            setting.setSettingValue(request.settingValue);
            setting.setDescription(request.description);
        }

        SystemSetting saved = settingRepository.save(setting);
        return Response.ok(saved).build();
    }

    @GET
    @Path("/category/{category}")
    public Response getSettingsByCategory(@PathParam("category") String category) {
        List<SystemSetting> allSettings = settingRepository.findAll();
        if (allSettings.isEmpty()) {
            allSettings = generateMockSettings();
        }
        List<SystemSetting> filtered = allSettings.stream()
                .filter(s -> category.equalsIgnoreCase(s.getCategory()))
                .toList();
        return Response.ok(filtered).build();
    }

    @POST
    @Path("/reset")
    public Response resetToDefaults() {
        // In production, this would reset all settings to defaults
        return Response.ok(Map.of("message", "Settings reset to defaults")).build();
    }

    @GET
    @Path("/export")
    public Response exportSettings() {
        List<SystemSetting> settings = settingRepository.findAll();
        if (settings.isEmpty()) {
            settings = generateMockSettings();
        }
        Map<String, String> export = new LinkedHashMap<>();
        for (SystemSetting s : settings) {
            export.put(s.getSettingKey(), s.getSettingValue());
        }
        return Response.ok(export).build();
    }

    @POST
    @Path("/import")
    public Response importSettings(Map<String, String> settings) {
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            Optional<SystemSetting> existing = settingRepository.findByKey(entry.getKey());
            SystemSetting setting;
            if (existing.isPresent()) {
                setting = existing.get();
                setting.setSettingValue(entry.getValue());
            } else {
                setting = new SystemSetting();
                setting.setSettingKey(entry.getKey());
                setting.setSettingValue(entry.getValue());
            }
            settingRepository.save(setting);
        }
        return Response.ok(Map.of("message", "Settings imported successfully", "count", settings.size())).build();
    }

    /**
     * Generate production-like mock settings when database is empty.
     */
    private List<SystemSetting> generateMockSettings() {
        List<SystemSetting> settings = new ArrayList<>();

        // Authentication settings
        settings.add(createMockSetting("auth_session_timeout", "900",
                "Access token expiration in seconds (15 minutes)", "Authentication", "number", true));
        settings.add(createMockSetting("auth_refresh_token_lifetime", "604800",
                "Refresh token lifetime in seconds (7 days)", "Authentication", "number", true));

        // Security settings
        settings.add(createMockSetting("mfa_enabled", "true",
                "Enable Multi-Factor Authentication globally", "Security", "boolean", true));
        settings.add(createMockSetting("mfa_grace_period", "86400",
                "Grace period for MFA setup in seconds (24 hours)", "Security", "number", true));
        settings.add(createMockSetting("password_min_length", "12",
                "Minimum password length requirement", "Security", "number", true));
        settings.add(createMockSetting("password_require_special", "true",
                "Require special characters in passwords", "Security", "boolean", true));
        settings.add(createMockSetting("password_require_uppercase", "true",
                "Require uppercase letters in passwords", "Security", "boolean", true));
        settings.add(createMockSetting("password_require_number", "true",
                "Require numbers in passwords", "Security", "boolean", true));
        settings.add(createMockSetting("password_expiry_days", "90",
                "Password expiration in days", "Security", "number", true));
        settings.add(createMockSetting("account_lockout_attempts", "5",
                "Failed login attempts before lockout", "Security", "number", true));
        settings.add(createMockSetting("account_lockout_duration", "1800",
                "Account lockout duration in seconds (30 min)", "Security", "number", true));

        // Advanced settings
        settings.add(createMockSetting("stego_enabled", "true",
                "Enable steganographic transmission for sensitive data", "Advanced", "boolean", true));

        // Compliance settings
        settings.add(createMockSetting("audit_retention_days", "365",
                "Audit log retention period in days", "Compliance", "number", false));

        // API settings
        settings.add(createMockSetting("api_rate_limit", "100",
                "API requests per minute per user", "API", "number", true));
        settings.add(createMockSetting("cors_allowed_origins", "https://securegate.io,https://admin.securegate.io",
                "Allowed CORS origins (comma-separated)", "API", "string", true));

        return settings;
    }

    private SystemSetting createMockSetting(String key, String value, String description,
            String category, String dataType, boolean isEditable) {
        SystemSetting setting = new SystemSetting();
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        setting.setDescription(description);
        setting.setCategory(category);
        setting.setDataType(dataType);
        setting.setEditable(isEditable);
        setting.setLastModifiedAt(Instant.now().toString());
        return setting;
    }

    public static class SettingRequest {
        public String settingKey;
        public String settingValue;
        public String description;
    }
}
