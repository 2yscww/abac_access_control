package com.xie.platform.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PolicyConfig {
    private Long policyId;
    private String policyName;
    private String description;
    private String conditions;
    private String effect;
    private Integer priority;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
