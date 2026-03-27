package com.xie.platform.access.policy.config;

import lombok.Data;

@Data
public class HistoricalExportPolicyConfig implements ToggleablePolicyConfig {
    private Boolean enabled = true;
    private Integer exportThreshold = 50;
    private Integer exportWindowMinutes = 30;
}
