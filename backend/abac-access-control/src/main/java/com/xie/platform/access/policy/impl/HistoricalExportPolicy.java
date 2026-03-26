package com.xie.platform.access.policy.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.environment.Environment;
import com.xie.platform.access.policy.Policy;
import com.xie.platform.access.policy.PolicyLayer;
import com.xie.platform.access.policy.PolicyResult;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.access.resource.ResourceType;
import com.xie.platform.access.subject.Subject;
import com.xie.platform.mapper.AuditLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 历史行为安全策略。
 *
 * <p>传统 ABAC 只看单次请求是否合法，这里额外引入“最近一段时间的成功导出次数”作为风险信号。
 * 这样即使员工对单个资产都有访问权限，也能阻断短时间内的大量导出行为。</p>
 */
@Component
public class HistoricalExportPolicy implements Policy {

    private static final int EXPORT_THRESHOLD = 50;
    private static final long EXPORT_WINDOW_MINUTES = 30;

    @Autowired
    private AuditLogMapper auditLogMapper;

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
        if (action != Action.EXPORT || resource.getType() != ResourceType.ASSET) {
            return PolicyResult.ALLOW;
        }
        if (subject.getEmployeeId() == null || environment == null || environment.getRequestTime() == null) {
            return PolicyResult.ALLOW;
        }

        LocalDateTime windowStart = environment.getRequestTime().minusMinutes(EXPORT_WINDOW_MINUTES);
        int recentExports = auditLogMapper.countRecentAllowedActions(
                subject.getEmployeeId(),
                Action.EXPORT.name(),
                ResourceType.ASSET.name(),
                windowStart
        );

        // The current request has not been recorded yet, so >= threshold means the next export should be blocked.
        return recentExports >= EXPORT_THRESHOLD ? PolicyResult.DENY : PolicyResult.ALLOW;
    }
}
