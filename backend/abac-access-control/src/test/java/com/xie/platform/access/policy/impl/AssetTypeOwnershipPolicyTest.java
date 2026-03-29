package com.xie.platform.access.policy.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.environment.Environment;
import com.xie.platform.access.policy.PolicyResult;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.access.resource.ResourceType;
import com.xie.platform.access.subject.Subject;
import com.xie.platform.model.enumValue.AssetType;
import com.xie.platform.model.enumValue.DeptType;
import com.xie.platform.model.enumValue.EmployeeLevel;
import com.xie.platform.model.enumValue.ProjectPhase;
import com.xie.platform.model.enumValue.SecurityLevel;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssetTypeOwnershipPolicyTest {

    @Test
    void evaluate_shouldAllowRdWritingSourceCode() {
        AssetTypeOwnershipPolicy policy = new AssetTypeOwnershipPolicy();

        PolicyResult result = policy.evaluate(
                new Subject(1L, 2L, DeptType.RD, 1L, EmployeeLevel.P6, false),
                buildAssetResource(AssetType.SOURCE_CODE),
                Action.WRITE,
                buildEnvironment()
        );

        assertEquals(PolicyResult.ALLOW, result);
    }

    @Test
    void evaluate_shouldDenyRdWritingOpsDoc() {
        AssetTypeOwnershipPolicy policy = new AssetTypeOwnershipPolicy();

        PolicyResult result = policy.evaluate(
                new Subject(1L, 2L, DeptType.RD, 1L, EmployeeLevel.P6, false),
                buildAssetResource(AssetType.OPS_DOC),
                Action.WRITE,
                buildEnvironment()
        );

        assertEquals(PolicyResult.DENY, result);
    }

    @Test
    void evaluate_shouldIgnoreManagementExportRule() {
        AssetTypeOwnershipPolicy policy = new AssetTypeOwnershipPolicy();

        PolicyResult result = policy.evaluate(
                new Subject(1L, 2L, DeptType.MANAGEMENT, 1L, EmployeeLevel.VP, false),
                buildAssetResource(AssetType.OPS_DOC),
                Action.EXPORT,
                buildEnvironment()
        );

        assertEquals(PolicyResult.ALLOW, result);
    }

    private Resource buildAssetResource(AssetType assetType) {
        return Resource.builder()
                .type(ResourceType.ASSET)
                .projectId(11L)
                .projectPhase(ProjectPhase.DEVELOPMENT)
                .assetsStage(ProjectPhase.DEVELOPMENT)
                .securityLevel(SecurityLevel.INTERNAL)
                .assetType(assetType)
                .build();
    }

    private Environment buildEnvironment() {
        return Environment.builder()
                .requestTime(LocalDateTime.of(2026, 3, 29, 10, 0, 0))
                .ipAddress("10.0.0.1")
                .requestUri("/api/asset/create")
                .build();
    }
}
