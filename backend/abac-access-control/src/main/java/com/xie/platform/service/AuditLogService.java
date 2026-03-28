package com.xie.platform.service;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.environment.Environment;
import com.xie.platform.access.pdp.DecisionResult;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.dto.AuditLogQueryDTO;

import java.util.Map;

public interface AuditLogService {

    void recordDecision(
            Long employeeId,
            Resource resource,
            Action action,
            Environment environment,
            DecisionResult decisionResult
    );

    void recordBusinessEvent(
            Long employeeId,
            String resourceType,
            Long resourceId,
            Action action,
            Map<String, Object> detail
    );

    void recordSecurityEvent(
            Long employeeId,
            String resourceType,
            Long resourceId,
            Action action,
            String denyReason,
            Map<String, Object> detail
    );

    Map<String, Object> queryAuditLogs(AuditLogQueryDTO query, Long operatorEmployeeId);
}
