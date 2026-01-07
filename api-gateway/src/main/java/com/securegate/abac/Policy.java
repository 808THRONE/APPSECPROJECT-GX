package com.securegate.abac;

import java.util.Map;

/**
 * ABAC Policy record
 */
public record Policy(
        String policyId,
        String effect, // "permit" or "deny"
        Map<String, Object> subject,
        Map<String, Object> resource,
        Map<String, Object> environment) {
    public Policy {
        if (policyId == null || policyId.isEmpty()) {
            throw new IllegalArgumentException("policyId is required");
        }
        if (effect == null || (!effect.equalsIgnoreCase("permit") && !effect.equalsIgnoreCase("deny"))) {
            throw new IllegalArgumentException("effect must be 'permit' or 'deny'");
        }
    }
}
