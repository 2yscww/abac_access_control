package com.xie.platform.access.policy.config;

import lombok.Data;

@Data
public class SecurityLevelPolicyConfig implements ToggleablePolicyConfig {
    private Boolean enabled = true;
    private Integer publicMinRank = 1;
    private Integer internalMinRank = 3;
    private Integer confidentialMinRank = 5;
    private Integer topSecretMinRank = 9;
}
