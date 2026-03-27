package com.xie.platform.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class PolicyConfigDTO {
    private String policyName;
    private String displayName;
    private String description;
    private Boolean enabled;
    private Integer priority;
    private Map<String, Object> conditions;
    private LocalDateTime updatedAt;
}
