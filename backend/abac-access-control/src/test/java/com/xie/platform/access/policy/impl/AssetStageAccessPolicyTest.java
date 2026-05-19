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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssetStageAccessPolicyTest {

    private AssetStageAccessPolicy policy;
    private Environment environment;

    @BeforeEach
    void setUp() {
        policy = new AssetStageAccessPolicy();
        environment = Environment.builder().build();
    }

    @Test
    void evaluate_shouldAllowRdReadDevelopmentAsset() {
        PolicyResult result = policy.evaluate(
                buildSubject(DeptType.RD),
                buildAsset(ProjectPhase.DEVELOPMENT),
                Action.READ,
                environment
        );

        assertEquals(PolicyResult.ALLOW, result);
    }

    @Test
    void evaluate_shouldDenyRdReadInitAsset() {
        PolicyResult result = policy.evaluate(
                buildSubject(DeptType.RD),
                buildAsset(ProjectPhase.INIT),
                Action.READ,
                environment
        );

        assertEquals(PolicyResult.DENY, result);
    }

    @Test
    void evaluate_shouldAllowManagementReadArchivedAsset() {
        PolicyResult result = policy.evaluate(
                buildSubject(DeptType.MANAGEMENT),
                buildAsset(ProjectPhase.ARCHIVED),
                Action.READ,
                environment
        );

        assertEquals(PolicyResult.ALLOW, result);
    }

    @Test
    void evaluate_shouldDenyManagementWriteArchivedAsset() {
        PolicyResult result = policy.evaluate(
                buildSubject(DeptType.MANAGEMENT),
                buildAsset(ProjectPhase.ARCHIVED),
                Action.WRITE,
                environment
        );

        assertEquals(PolicyResult.DENY, result);
    }

    @Test
    void evaluate_shouldAllowNonAssetResources() {
        Resource projectResource = Resource.builder()
                .type(ResourceType.PROJECT)
                .projectPhase(ProjectPhase.TEST)
                .securityLevel(SecurityLevel.INTERNAL)
                .build();

        PolicyResult result = policy.evaluate(
                buildSubject(DeptType.QA),
                projectResource,
                Action.READ,
                environment
        );

        assertEquals(PolicyResult.ALLOW, result);
    }

    @Test
    void evaluate_shouldDenyWhenAssetStageMissing() {
        Resource resource = Resource.builder()
                .type(ResourceType.ASSET)
                .assetsStage(null)
                .assetType(AssetType.SOURCE_CODE)
                .securityLevel(SecurityLevel.INTERNAL)
                .build();

        PolicyResult result = policy.evaluate(
                buildSubject(DeptType.RD),
                resource,
                Action.READ,
                environment
        );

        assertEquals(PolicyResult.DENY, result);
    }

    @Test
    void evaluate_shouldAllowQaWriteTestAsset() {
        PolicyResult result = policy.evaluate(
                buildSubject(DeptType.QA),
                buildAsset(ProjectPhase.TEST),
                Action.WRITE,
                environment
        );

        assertEquals(PolicyResult.ALLOW, result);
    }

    private Subject buildSubject(DeptType deptType) {
        return new Subject(1L, 1L, deptType, 1L, EmployeeLevel.P6, false);
    }

    private Resource buildAsset(ProjectPhase assetsStage) {
        return Resource.builder()
                .type(ResourceType.ASSET)
                .projectId(11L)
                .resourceId(22L)
                .projectPhase(ProjectPhase.DEVELOPMENT)
                .assetsStage(assetsStage)
                .assetType(AssetType.SOURCE_CODE)
                .securityLevel(SecurityLevel.INTERNAL)
                .build();
    }
}
