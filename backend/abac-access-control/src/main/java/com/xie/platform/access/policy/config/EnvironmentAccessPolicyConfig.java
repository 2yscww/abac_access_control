package com.xie.platform.access.policy.config;

import lombok.Data;

@Data
public class EnvironmentAccessPolicyConfig implements ToggleablePolicyConfig {
    private Boolean enabled = true;
    private String workStart = "08:00";
    private String workEnd = "20:00";
}
