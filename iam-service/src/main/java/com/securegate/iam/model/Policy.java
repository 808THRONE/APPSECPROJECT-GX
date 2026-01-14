package com.securegate.iam.model;

import jakarta.persistence.*;
import jakarta.json.bind.annotation.JsonbProperty;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "policies")
public class Policy implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "policyId")
    private UUID policyId;

    @Column(name = "priority", nullable = false)
    @JsonbProperty("priority")
    private int priority = 1;

    @Column(nullable = false, unique = true)
    @JsonbProperty("name")
    private String name;

    @Column(columnDefinition = "TEXT")
    @JsonbProperty("description")
    private String description;

    @Column(nullable = false)
    @JsonbProperty("effect")
    private String effect; // ALLOW, DENY

    @Column(name = "risk_level")
    @JsonbProperty("riskLevel")
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "created_by")
    @JsonbProperty("createdBy")
    private String createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "is_active", nullable = false)
    @JsonbProperty("active")
    private boolean active = true;

    @Column(nullable = false)
    @JsonbProperty("resource")
    private String resource; // e.g., "USER", "*", "POLICY"

    @Column(nullable = false)
    @JsonbProperty("action")
    private String action; // e.g., "CREATE", "DELETE"

    @Column(name = "conditions")
    @JsonbProperty("conditions")
    private String conditions; // e.g., "role=ADMIN"

    // Getters and Setters
    public UUID getPolicyId() {
        return policyId;
    }

    public void setPolicyId(UUID policyId) {
        this.policyId = policyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEffect() {
        return effect;
    }

    public void setEffect(String effect) {
        this.effect = effect;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
