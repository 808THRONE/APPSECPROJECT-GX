package com.securegate.iam.service;

import com.securegate.iam.model.Role;
import com.securegate.iam.model.User;
import com.securegate.iam.repository.RoleRepository;
import com.securegate.iam.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import java.util.logging.Logger;
import com.securegate.iam.model.SystemSetting;
import com.securegate.iam.model.Notification;
import com.securegate.iam.model.AuditLog;
import com.securegate.iam.repository.SystemSettingRepository;
import com.securegate.iam.repository.NotificationRepository;
import com.securegate.iam.repository.AuditLogRepository;
import com.securegate.iam.model.Policy;
import com.securegate.iam.repository.PolicyRepository;
import java.time.LocalDateTime;

@Singleton
@Startup
public class DatabaseInitializer {

    private static final Logger LOGGER = Logger.getLogger(DatabaseInitializer.class.getName());

    @Inject
    private UserRepository userRepository;

    @Inject
    private RoleRepository roleRepository;

    @Inject
    private PolicyRepository policyRepository;

    @Inject
    private SystemSettingRepository settingRepository;

    @Inject
    private NotificationRepository notificationRepository;

    @Inject
    private AuditLogRepository auditLogRepository;

    @Inject
    private CryptoService cryptoService;

    @PostConstruct
    public void init() {
        LOGGER.info("Starting Database Initialization...");

        // 1. Roles
        Role adminRole = getOrCreateRole("ADMIN", "Full system access");
        Role managerRole = getOrCreateRole("MANAGER", "Department management");
        Role userRole = getOrCreateRole("USER", "Standard access");
        getOrCreateRole("AUDITOR", "Read-only audit access");

        // 2. Users
        createUser("admin", "admin@securegate.io", "System Administrator", "admin123", adminRole, userRole);
        createUser("808throne", "808throne@securegate.io", "808 Throne (Super Admin)", "808throne", adminRole,
                userRole);
        createUser("alice", "alice@securegate.io", "Alice (Security Admin)", "alice123", adminRole);
        createUser("bob", "bob@securegate.io", "Bob (Finance Manager)", "bob123", managerRole, userRole);
        createUser("charlie", "charlie@securegate.io", "Charlie (Standard User)", "charlie123", userRole);

        // 3. Policies
        createPolicy("Global Deny All", "DENY", "*", "*", "priority=100", true, 100);
        createPolicy("Admin Full Access", "PERMIT", "*", "*", "role=ADMIN", true, 1);
        createPolicy("Manager Audit Read", "PERMIT", "AUDIT_LOG", "READ", "role=MANAGER", true, 10);
        createPolicy("Auditor Full Read", "PERMIT", "*", "READ", "role=AUDITOR", true, 5);
        createPolicy("Standard User Profile Read", "PERMIT", "USER_PROFILE", "READ", "role=USER", true, 20);

        // 4. System Settings
        seedSettings();

        // 5. Notifications
        seedNotifications();

        // 6. Audit Logs
        seedAuditLogs();

        LOGGER.info("Database Initialization Complete.");
    }

    private void seedSettings() {
        if (settingRepository.findAll().isEmpty()) {
            saveSetting("auth_session_timeout", "900", "Access token expiration (15 min)", "Authentication", "number");
            saveSetting("mfa_enabled", "true", "Enable MFA globally", "Security", "boolean");
            saveSetting("password_min_length", "12", "Min password length", "Security", "number");
            saveSetting("stego_enabled", "true", "Enable steganography", "Advanced", "boolean");
            saveSetting("api_rate_limit", "100", "Requests per minute", "API", "number");
        }
    }

    private void seedNotifications() {
        if (notificationRepository.findAll().isEmpty()) {
            saveNotification("System Deployed", "SecureGate IAM Portal v1.0.0 is operational.", "SUCCESS", "System");
            saveNotification("Security Patch", "CVE-2026-0001 patch applied.", "INFO", "Security");
            saveNotification("New Policy", "GeoRestriction policy activated.", "INFO", "Policy");
        }
    }

    private void seedAuditLogs() {
        if (auditLogRepository.findAll().isEmpty()) {
            saveAuditLog("SYSTEM", "STARTUP", "SYSTEM", "SecureGate IAM Portal v1.0.0 started.", "success");
            saveAuditLog("admin", "LOGIN_SUCCESS", "SESSION", "Admin login successful via MFA.", "success");
            saveAuditLog("system", "INITIALIZATION", "DATABASE", "Mock data seeding complete.", "success");
        }
    }

    private void saveSetting(String key, String val, String desc, String cat, String type) {
        SystemSetting s = new SystemSetting();
        s.setSettingKey(key);
        s.setSettingValue(val);
        s.setDescription(desc);
        s.setCategory(cat);
        s.setDataType(type);
        s.setEditable(true);
        settingRepository.save(s);
    }

    private void saveNotification(String title, String msg, String type, String cat) {
        Notification n = new Notification();
        n.setTitle(title);
        n.setMessage(msg);
        n.setType(type);
        n.setCategory(cat);
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(n);
    }

    private void saveAuditLog(String actor, String action, String resource, String details, String status) {
        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setAction(action);
        log.setResource(resource);
        log.setDetails(details);
        log.setStatus(status);
        log.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    private Role getOrCreateRole(String name, String desc) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role r = new Role();
            r.setRoleName(name);
            r.setDescription(desc);
            return roleRepository.save(r);
        });
    }

    private void createUser(String username, String email, String fullName, String pass, Role... roles) {
        if (userRepository.findByUsername(username).isEmpty()) {
            User u = new User();
            u.setUsername(username);
            u.setEmail(email);
            u.setFullName(fullName);
            u.setStatus("ACTIVE");
            u.setPasswordHash(cryptoService.hashPassword(pass));
            for (Role r : roles)
                u.getRoles().add(r);
            userRepository.save(u);
        }
    }

    private void createPolicy(String name, String effect, String resource, String action, String conditions,
            boolean active, int priority) {
        if (policyRepository.findAll().stream().noneMatch(p -> p.getName().equals(name))) {
            Policy p = new Policy();
            p.setName(name);
            p.setEffect(effect);
            p.setResource(resource);
            p.setAction(action);
            p.setConditions(conditions);
            p.setActive(active);
            p.setPriority(priority);
            policyRepository.save(p);
        }
    }
}
