package com.xie.platform.access.policy.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.environment.Environment;
import com.xie.platform.access.policy.PolicyResult;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.access.resource.ResourceType;
import com.xie.platform.access.subject.Subject;
import com.xie.platform.model.enumValue.DeptType;
import com.xie.platform.model.enumValue.EmployeeLevel;
import com.xie.platform.model.enumValue.ProjectPhase;
import com.xie.platform.model.enumValue.SecurityLevel;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnvironmentAccessPolicyTest {

    private final EnvironmentAccessPolicy policy = new EnvironmentAccessPolicy();

    @Test
    void evaluate_shouldDenyHighSecurityAccessOutsideWorkingHours() {
        PolicyResult result = policy.evaluate(
                buildSubject(),
                buildHighSecurityProject(),
                Action.READ,
                buildEnvironment(LocalDateTime.of(2026, 3, 19, 22, 0), "10.0.0.5")
        );

        assertEquals(PolicyResult.DENY, result);
    }

    @Test
    void evaluate_shouldDenyHighSecurityAssetReadFromPublicIp() {
        PolicyResult result = policy.evaluate(
                buildSubject(),
                buildHighSecurityAsset(),
                Action.READ,
                buildEnvironment(LocalDateTime.of(2026, 3, 19, 10, 0), "8.8.8.8")
        );

        assertEquals(PolicyResult.DENY, result);
    }

    @Test
    void evaluate_shouldAllowHighSecurityAssetExportFromInternalIpDuringWorkingHours() {
        PolicyResult result = policy.evaluate(
                buildSubject(),
                buildHighSecurityAsset(),
                Action.EXPORT,
                buildEnvironment(LocalDateTime.of(2026, 3, 19, 10, 0), "192.168.10.25")
        );

        assertEquals(PolicyResult.ALLOW, result);
    }

    @Test
    void evaluate_shouldAllowLowSecurityAccessFromPublicIpOutsideRestrictedScenarios() {
        Resource lowSecurityAsset = Resource.builder()
                .type(ResourceType.ASSET)
                .resourceId(88L)
                .projectId(11L)
                .projectPhase(ProjectPhase.DEVELOPMENT)
                .assetsStage(ProjectPhase.DEVELOPMENT)
                .securityLevel(SecurityLevel.INTERNAL)
                .build();

        PolicyResult result = policy.evaluate(
                buildSubject(),
                lowSecurityAsset,
                Action.READ,
                buildEnvironment(LocalDateTime.of(2026, 3, 19, 22, 0), "8.8.8.8")
        );

        assertEquals(PolicyResult.ALLOW, result);
    }

    private Subject buildSubject() {
        return new Subject(7L, 2L, DeptType.RD, 1L, EmployeeLevel.P6, false);
    }

    private Resource buildHighSecurityProject() {
        return Resource.builder()
                .type(ResourceType.PROJECT)
                .resourceId(11L)
                .projectId(11L)
                .projectPhase(ProjectPhase.DEVELOPMENT)
                .securityLevel(SecurityLevel.CONFIDENTIAL)
                .build();
    }

    private Resource buildHighSecurityAsset() {
        return Resource.builder()
                .type(ResourceType.ASSET)
                .resourceId(88L)
                .projectId(11L)
                .projectPhase(ProjectPhase.DEVELOPMENT)
                .assetsStage(ProjectPhase.DEVELOPMENT)
                .securityLevel(SecurityLevel.CONFIDENTIAL)
                .build();
    }

    private Environment buildEnvironment(LocalDateTime requestTime, String ipAddress) {
        return Environment.builder()
                .requestTime(requestTime)
                .ipAddress(ipAddress)
                .requestUri("/api/test")
                .build();
    }
}
