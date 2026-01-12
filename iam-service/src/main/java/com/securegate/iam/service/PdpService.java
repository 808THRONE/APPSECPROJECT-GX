package com.securegate.iam.service;

import com.securegate.iam.model.Policy;
import com.securegate.iam.model.User;
import com.securegate.iam.repository.PolicyRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@ApplicationScoped
public class PdpService {

    private static final Logger LOGGER = Logger.getLogger(PdpService.class.getName());

    @Inject
    private PolicyRepository policyRepository;

    /**
     * Evaluate access request against policies in DB.
     * 
     * @param user     The user requesting access
     * @param resource The resource being accessed (e.g., "USER", "PAYMENT")
     * @param action   The action being performed (e.g., "DELETE", "APPROVE")
     * @param context  Additional context (not fully used in this MVP)
     * @return true if access is PERMITTED
     */
    public boolean evaluate(User user, String resource, String action, Map<String, Object> context) {

        // 1. Super admin always allowed (Implicit Policy or keep hardcoded safety)
        if (user.getRoles().stream().anyMatch(r -> r.getRoleName().equals("ADMIN"))) {
            return true;
        }

        // 2. Fetch all policies (In prod, filter by active & resource/action in SQL)
        List<Policy> allPolicies = policyRepository.findAll();

        boolean permit = false;

        for (Policy policy : allPolicies) {
            if (!policy.isActive())
                continue;

            // Match Resource & Action (Simple String Match or Wildcard *)
            if (matches(policy.getResource(), resource) && matches(policy.getAction(), action)) {

                // Check Conditions (Simplified: condition is "role=NAME")
                if (checkCondition(policy.getConditions(), user)) {
                    if ("DENY".equalsIgnoreCase(policy.getEffect())) {
                        return false; // Explicit Deny wins
                    }
                    if ("PERMIT".equalsIgnoreCase(policy.getEffect())) {
                        permit = true;
                    }
                }
            }
        }

        return permit;
    }

    private boolean matches(String policyVal, String requestVal) {
        if (policyVal == null)
            return false;
        if ("*".equals(policyVal))
            return true;
        return policyVal.equalsIgnoreCase(requestVal);
    }

    private boolean checkCondition(String condition, User user) {
        if (condition == null || condition.isBlank())
            return true; // No condition = applies to everyone

        // Simple syntax: "role=MANAGER"
        if (condition.startsWith("role=")) {
            String requiredRole = condition.substring(5).trim();
            return user.getRoles().stream().anyMatch(r -> r.getRoleName().equalsIgnoreCase(requiredRole));
        }

        // Simple syntax: "department=Finance" (Assuming we had department in
        // User/Roles, assume Role mapping for now)
        // For MVP, we only support role-based conditions in ABAC engine
        return false;
    }
}
