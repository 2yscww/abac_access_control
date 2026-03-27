package com.xie.platform.access.policy.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.environment.Environment;
import com.xie.platform.access.policy.Policy;
import com.xie.platform.access.policy.PolicyLayer;
import com.xie.platform.access.policy.PolicyResult;
import com.xie.platform.access.policy.config.EnvironmentAccessPolicyConfig;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.access.resource.ResourceType;
import com.xie.platform.access.subject.Subject;
import com.xie.platform.model.enumValue.SecurityLevel;
import com.xie.platform.service.PolicyConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.regex.Pattern;

@Component
public class EnvironmentAccessPolicy implements Policy {

    private static final Pattern PRIVATE_172_RANGE =
            Pattern.compile("^172\\.(1[6-9]|2\\d|3[0-1])\\..*");

    @Autowired(required = false)
    private PolicyConfigService policyConfigService;

    @Override
    public String getName() {
        return "EnvironmentAccessPolicy";
    }

    @Override
    public PolicyLayer getLayer() {
        return PolicyLayer.SECURITY;
    }

    @Override
    public int getOrder() {
        return 20;
    }

    @Override
    public PolicyResult evaluate(Subject subject, Resource resource, Action action, Environment environment) {
        EnvironmentAccessPolicyConfig config = resolveConfig();
        if (Boolean.FALSE.equals(config.getEnabled())) {
            return PolicyResult.ALLOW;
        }

        SecurityLevel securityLevel = resource.getSecurityLevel();
        if (!isHighSecurity(securityLevel)) {
            return PolicyResult.ALLOW;
        }

        if (!isWithinWorkingHours(environment != null ? environment.getRequestTime() : null, config)) {
            return PolicyResult.DENY;
        }

        if (resource.getType() == ResourceType.ASSET
                && isNetworkRestrictedAction(action)
                && !isInternalIp(environment != null ? environment.getIpAddress() : null)) {
            return PolicyResult.DENY;
        }

        return PolicyResult.ALLOW;
    }

    private boolean isHighSecurity(SecurityLevel securityLevel) {
        return securityLevel != null && securityLevel.getLevel() >= SecurityLevel.CONFIDENTIAL.getLevel();
    }

    private boolean isWithinWorkingHours(LocalDateTime requestTime, EnvironmentAccessPolicyConfig config) {
        if (requestTime == null) {
            return false;
        }

        LocalTime workStart = parseTime(config.getWorkStart(), LocalTime.of(8, 0));
        LocalTime workEnd = parseTime(config.getWorkEnd(), LocalTime.of(20, 0));
        LocalTime currentTime = requestTime.toLocalTime();
        return !currentTime.isBefore(workStart) && !currentTime.isAfter(workEnd);
    }

    private boolean isNetworkRestrictedAction(Action action) {
        return action == Action.READ || action == Action.EXPORT;
    }

    private boolean isInternalIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return false;
        }

        String ip = ipAddress.trim();
        if ("unknown".equalsIgnoreCase(ip)) {
            return false;
        }

        return ip.startsWith("10.")
                || ip.startsWith("192.168.")
                || PRIVATE_172_RANGE.matcher(ip).matches()
                || "127.0.0.1".equals(ip)
                || "::1".equals(ip)
                || "0:0:0:0:0:0:0:1".equals(ip)
                || ip.startsWith("fc")
                || ip.startsWith("fd");
    }

    private EnvironmentAccessPolicyConfig resolveConfig() {
        EnvironmentAccessPolicyConfig config = policyConfigService != null
                ? policyConfigService.getEnvironmentAccessPolicyConfig()
                : null;
        return config != null ? config : new EnvironmentAccessPolicyConfig();
    }

    private LocalTime parseTime(String value, LocalTime fallback) {
        try {
            return LocalTime.parse(value);
        } catch (Exception exception) {
            return fallback;
        }
    }
}
