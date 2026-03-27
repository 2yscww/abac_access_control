package com.xie.platform.access.policy.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.environment.Environment;
import com.xie.platform.access.policy.Policy;
import com.xie.platform.access.policy.PolicyLayer;
import com.xie.platform.access.policy.PolicyResult;
import com.xie.platform.access.policy.config.HistoricalExportPolicyConfig;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.access.resource.ResourceType;
import com.xie.platform.access.subject.Subject;
import com.xie.platform.mapper.AuditLogMapper;
import com.xie.platform.service.PolicyConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class HistoricalExportPolicy implements Policy {

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Autowired(required = false)
    private PolicyConfigService policyConfigService;

    @Override
    public String getName() {
        return "HistoricalExportPolicy";
    }

    @Override
    public PolicyLayer getLayer() {
        return PolicyLayer.SECURITY;
    }

    @Override
    public int getOrder() {
        return 30;
    }

    @Override
    public PolicyResult evaluate(Subject subject, Resource resource, Action action, Environment environment) {
        HistoricalExportPolicyConfig config = resolveConfig();
        if (Boolean.FALSE.equals(config.getEnabled())) {
            return PolicyResult.ALLOW;
        }

        if (action != Action.EXPORT || resource.getType() != ResourceType.ASSET) {
            return PolicyResult.ALLOW;
        }
        if (subject.getEmployeeId() == null || environment == null || environment.getRequestTime() == null) {
            return PolicyResult.ALLOW;
        }

        LocalDateTime windowStart = environment.getRequestTime().minusMinutes(config.getExportWindowMinutes());
        int recentExports = auditLogMapper.countRecentAllowedActions(
                subject.getEmployeeId(),
                Action.EXPORT.name(),
                ResourceType.ASSET.name(),
                windowStart
        );

        return recentExports >= config.getExportThreshold() ? PolicyResult.DENY : PolicyResult.ALLOW;
    }

    private HistoricalExportPolicyConfig resolveConfig() {
        HistoricalExportPolicyConfig config = policyConfigService != null
                ? policyConfigService.getHistoricalExportPolicyConfig()
                : null;
        return config != null ? config : new HistoricalExportPolicyConfig();
    }
}
