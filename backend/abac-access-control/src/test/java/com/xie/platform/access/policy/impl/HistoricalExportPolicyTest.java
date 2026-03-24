package com.xie.platform.access.policy.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.environment.Environment;
import com.xie.platform.access.policy.PolicyResult;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.access.resource.ResourceType;
import com.xie.platform.access.subject.Subject;
import com.xie.platform.mapper.AuditLogMapper;
import com.xie.platform.model.enumValue.DeptType;
import com.xie.platform.model.enumValue.EmployeeLevel;
import com.xie.platform.model.enumValue.ProjectPhase;
import com.xie.platform.model.enumValue.SecurityLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoricalExportPolicyTest {

    @Mock
    private AuditLogMapper auditLogMapper;

    @InjectMocks
    private HistoricalExportPolicy policy;

    @Test
    void evaluate_shouldAllowWhenRecentExportsAreBelowThreshold() {
        when(auditLogMapper.countRecentAllowedActions(eq(7L), eq("EXPORT"), eq("ASSET"), any(LocalDateTime.class)))
                .thenReturn(49);

        PolicyResult result = policy.evaluate(buildSubject(), buildAssetResource(), Action.EXPORT, buildEnvironment());

        assertEquals(PolicyResult.ALLOW, result);
    }

    @Test
    void evaluate_shouldDenyWhenRecentExportsReachThreshold() {
        when(auditLogMapper.countRecentAllowedActions(eq(7L), eq("EXPORT"), eq("ASSET"), any(LocalDateTime.class)))
                .thenReturn(50);

        PolicyResult result = policy.evaluate(buildSubject(), buildAssetResource(), Action.EXPORT, buildEnvironment());

        assertEquals(PolicyResult.DENY, result);
    }

    @Test
    void evaluate_shouldIgnoreNonExportActions() {
        PolicyResult result = policy.evaluate(buildSubject(), buildAssetResource(), Action.READ, buildEnvironment());

        assertEquals(PolicyResult.ALLOW, result);
        verifyNoInteractions(auditLogMapper);
    }

    private Subject buildSubject() {
        return new Subject(7L, 2L, DeptType.RD, 1L, EmployeeLevel.P6, false);
    }

    private Resource buildAssetResource() {
        return Resource.builder()
                .type(ResourceType.ASSET)
                .resourceId(101L)
                .projectId(11L)
                .projectPhase(ProjectPhase.DEVELOPMENT)
                .assetsStage(ProjectPhase.DEVELOPMENT)
                .securityLevel(SecurityLevel.INTERNAL)
                .build();
    }

    private Environment buildEnvironment() {
        return Environment.builder()
                .requestTime(LocalDateTime.of(2026, 3, 19, 10, 0, 0))
                .ipAddress("10.0.0.1")
                .requestUri("/api/asset/101/export")
                .build();
    }
}
