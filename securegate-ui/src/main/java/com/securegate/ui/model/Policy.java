package com.securegate.ui.model;

import java.io.Serializable;
import java.util.UUID;
import lombok.Data;

@Data
public class Policy implements Serializable {
    private UUID policyId;
    private String name;
    private String description;
    private String effect;
    private String riskLevel;
    private int priority;
    private String createdBy;
    private boolean active;
    private String resource;
    private String action;
    private String conditions;
}
