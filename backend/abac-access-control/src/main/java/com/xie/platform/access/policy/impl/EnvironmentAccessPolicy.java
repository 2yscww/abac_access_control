package com.xie.platform.access.policy.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.environment.Environment;
import com.xie.platform.access.policy.Policy;
import com.xie.platform.access.policy.PolicyLayer;
import com.xie.platform.access.policy.PolicyResult;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.access.resource.ResourceType;
import com.xie.platform.access.subject.Subject;
import com.xie.platform.model.enumValue.SecurityLevel;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.regex.Pattern;

/**
 * 环境策略。
 *
 * <p>第一版先落地两条容易解释、容易演示的规则：</p>
 * <p>1. 机密及以上资源只能在工作时间访问。</p>
 * <p>2. 机密及以上资产只能从内网执行 READ / EXPORT。</p>
 */
@Component
public class EnvironmentAccessPolicy implements Policy {

    private static final LocalTime WORK_START = LocalTime.of(8, 0);
    private static final LocalTime WORK_END = LocalTime.of(20, 0);
    private static final Pattern PRIVATE_172_RANGE =
            Pattern.compile("^172\\.(1[6-9]|2\\d|3[0-1])\\..*");

    @Override
    public String getName() {
        return "EnvironmentAccessPolicy";
    }

    @Override
    public PolicyLayer getLayer() {
        return PolicyLayer.SECURITY;
    }

    @Override
    public PolicyResult evaluate(Subject subject, Resource resource, Action action, Environment environment) {
        SecurityLevel securityLevel = resource.getSecurityLevel();
        if (!isHighSecurity(securityLevel)) {
            return PolicyResult.ALLOW;
        }

        if (!isWithinWorkingHours(environment != null ? environment.getRequestTime() : null)) {
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

    private boolean isWithinWorkingHours(LocalDateTime requestTime) {
        if (requestTime == null) {
            return false;
        }

        LocalTime currentTime = requestTime.toLocalTime();
        return !currentTime.isBefore(WORK_START) && !currentTime.isAfter(WORK_END);
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
}
