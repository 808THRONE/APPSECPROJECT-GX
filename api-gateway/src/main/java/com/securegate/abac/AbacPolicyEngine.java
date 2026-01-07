package com.securegate.abac;

import java.time.LocalTime;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * ABAC Policy Engine - Evaluates access decisions based on subject, resource,
 * and environment attributes
 */
public class AbacPolicyEngine {

    private static final Logger LOGGER = Logger.getLogger(AbacPolicyEngine.class.getName());

    /**
     * Evaluate access decision
     * 
     * @param subject     User attributes (department, clearance_level, roles, etc.)
     * @param resource    Resource attributes (type, sensitivity, owner, etc.)
     * @param environment Environment attributes (time, location, device, etc.)
     * @param policy      Policy to evaluate against
     * @return true if access is permitted, false otherwise
     */
    public boolean evaluate(Map<String, Object> subject, Map<String, Object> resource,
            Map<String, Object> environment, Policy policy) {

        LOGGER.fine("Evaluating policy: " + policy.policyId());

        // Check subject conditions
        if (!matchesSubject(subject, policy.subject())) {
            LOGGER.fine("Subject conditions not met");
            return false;
        }

        // Check resource conditions
        if (!matchesResource(resource, policy.resource())) {
            LOGGER.fine("Resource conditions not met");
            return false;
        }

        // Check environment conditions (optional)
        if (policy.environment() != null && !policy.environment().isEmpty()) {
            if (!matchesEnvironment(environment, policy.environment())) {
                LOGGER.fine("Environment conditions not met");
                return false;
            }
        }

        LOGGER.fine("Policy matched: " + policy.policyId() + " -> " + policy.effect());
        return "permit".equalsIgnoreCase(policy.effect());
    }

    /**
     * Evaluate against all policies
     */
    public boolean evaluate(Map<String, Object> subject, Map<String, Object> resource,
            Map<String, Object> environment) {
        // Default deny if no policies match
        // In production, load from database
        return true; // Default allow for demo
    }

    private boolean matchesSubject(Map<String, Object> subject, Map<String, Object> policySubject) {
        if (policySubject == null || policySubject.isEmpty()) {
            return true;
        }

        for (Map.Entry<String, Object> entry : policySubject.entrySet()) {
            String key = entry.getKey();
            Object policyValue = entry.getValue();
            Object subjectValue = subject.get(key);

            if (subjectValue == null) {
                return false;
            }

            if (!matchesValue(subjectValue, policyValue)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesResource(Map<String, Object> resource, Map<String, Object> policyResource) {
        if (policyResource == null || policyResource.isEmpty()) {
            return true;
        }

        for (Map.Entry<String, Object> entry : policyResource.entrySet()) {
            String key = entry.getKey();
            Object policyValue = entry.getValue();
            Object resourceValue = resource.get(key);

            if (resourceValue == null) {
                return false;
            }

            if (!matchesValue(resourceValue, policyValue)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesEnvironment(Map<String, Object> environment, Map<String, Object> policyEnv) {
        if (policyEnv == null || policyEnv.isEmpty()) {
            return true;
        }

        // Check time constraints
        if (policyEnv.containsKey("time")) {
            @SuppressWarnings("unchecked")
            Map<String, String> timeConstraint = (Map<String, String>) policyEnv.get("time");
            String start = timeConstraint.get("start");
            String end = timeConstraint.get("end");

            if (start != null && end != null) {
                LocalTime now = LocalTime.now();
                LocalTime startTime = LocalTime.parse(start);
                LocalTime endTime = LocalTime.parse(end);

                if (now.isBefore(startTime) || now.isAfter(endTime)) {
                    return false;
                }
            }
        }

        // Check location constraints
        if (policyEnv.containsKey("location")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> locationConstraint = (Map<String, Object>) policyEnv.get("location");

            if (locationConstraint.containsKey("countries")) {
                @SuppressWarnings("unchecked")
                Set<String> allowedCountries = Set.copyOf((java.util.List<String>) locationConstraint.get("countries"));
                String userCountry = (String) environment.get("country");

                if (userCountry == null || !allowedCountries.contains(userCountry)) {
                    return false;
                }
            }
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private boolean matchesValue(Object actual, Object expected) {
        if (expected instanceof java.util.List) {
            // Policy expects one of the listed values
            java.util.List<Object> expectedList = (java.util.List<Object>) expected;
            if (actual instanceof java.util.Collection) {
                // Subject has multiple values, check for intersection
                java.util.Collection<Object> actualSet = (java.util.Collection<Object>) actual;
                return actualSet.stream().anyMatch(expectedList::contains);
            }
            return expectedList.contains(actual);
        }

        if (expected instanceof Map) {
            // Range check for numeric values
            Map<String, Object> range = (Map<String, Object>) expected;
            if (range.containsKey("min")) {
                int min = ((Number) range.get("min")).intValue();
                int actualValue = ((Number) actual).intValue();
                if (actualValue < min) {
                    return false;
                }
            }
            if (range.containsKey("max")) {
                int max = ((Number) range.get("max")).intValue();
                int actualValue = ((Number) actual).intValue();
                if (actualValue > max) {
                    return false;
                }
            }
            return true;
        }

        return actual.equals(expected);
    }
}
