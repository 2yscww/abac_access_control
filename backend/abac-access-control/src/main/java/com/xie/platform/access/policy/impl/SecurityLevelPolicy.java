package com.xie.platform.access.policy.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.environment.Environment;
import com.xie.platform.access.policy.Policy;
import com.xie.platform.access.policy.PolicyLayer;
import com.xie.platform.access.policy.PolicyResult;
import com.xie.platform.access.policy.config.SecurityLevelPolicyConfig;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.access.subject.Subject;
import com.xie.platform.model.enumValue.SecurityLevel;
import com.xie.platform.service.PolicyConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SecurityLevelPolicy implements Policy {

    @Autowired(required = false)
    private PolicyConfigService policyConfigService;

    @Override
    public String getName() {
        return "SecurityLevelPolicy";
    }

    @Override
    public PolicyLayer getLayer() {
        return PolicyLayer.SECURITY;
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public PolicyResult evaluate(Subject subject, Resource resource, Action action, Environment environment) {
        SecurityLevelPolicyConfig config = resolveConfig();
        if (Boolean.FALSE.equals(config.getEnabled())) {
            return PolicyResult.ALLOW;
        }

        SecurityLevel securityLevel = resource.getSecurityLevel();
        if (securityLevel == null || subject.getLevel() == null) {
            return PolicyResult.ALLOW;
        }

        if (Boolean.TRUE.equals(subject.getIsContractor())
                && securityLevel.getLevel() >= SecurityLevel.CONFIDENTIAL.getLevel()) {
            return PolicyResult.DENY;
        }

        int employeeRank = subject.getLevel().getRank();
        int requiredRank = getRequiredRank(securityLevel, config);
        return employeeRank >= requiredRank ? PolicyResult.ALLOW : PolicyResult.DENY;
    }

    private int getRequiredRank(SecurityLevel securityLevel, SecurityLevelPolicyConfig config) {
        return switch (securityLevel) {
            case PUBLIC -> config.getPublicMinRank();
            case INTERNAL -> config.getInternalMinRank();
            case CONFIDENTIAL -> config.getConfidentialMinRank();
            case TOP_SECRET -> config.getTopSecretMinRank();
        };
    }

    private SecurityLevelPolicyConfig resolveConfig() {
        SecurityLevelPolicyConfig config = policyConfigService != null
                ? policyConfigService.getSecurityLevelPolicyConfig()
                : null;
        return config != null ? config : new SecurityLevelPolicyConfig();
    }
}
