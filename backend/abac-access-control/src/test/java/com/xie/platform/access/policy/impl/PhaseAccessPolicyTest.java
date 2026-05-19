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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PhaseAccessPolicyTest {

    private PhaseAccessPolicy policy;
    private Environment environment;

    @BeforeEach
    void setUp() {
        policy = new PhaseAccessPolicy();
        environment = Environment.builder().build();
    }

    @Test
    void evaluate_shouldAllowProductWriteInInitPhase() {
        PolicyResult result = policy.evaluate(
                buildSubject(DeptType.PRODUCT),
                buildProject(ProjectPhase.INIT),
                Action.WRITE,
                environment
        );

        assertEquals(PolicyResult.ALLOW, result);
    }

    @Test
    void evaluate_shouldAllowManagementReadInDevelopmentPhase() {
        PolicyResult result = policy.evaluate(
                buildSubject(DeptType.MANAGEMENT),
                buildProject(ProjectPhase.DEVELOPMENT),
                Action.READ,
                environment
        );

        assertEquals(PolicyResult.ALLOW, result);
    }

    @Test
    void evaluate_shouldDenyManagementWriteInDevelopmentPhase() {
        PolicyResult result = policy.evaluate(
                buildSubject(DeptType.MANAGEMENT),
                buildProject(ProjectPhase.DEVELOPMENT),
                Action.WRITE,
                environment
        );

        assertEquals(PolicyResult.DENY, result);
    }

    @Test
    void evaluate_shouldDenyRdReadInInitPhase() {
        PolicyResult result = policy.evaluate(
                buildSubject(DeptType.RD),
                buildProject(ProjectPhase.INIT),
                Action.READ,
                environment
        );

        assertEquals(PolicyResult.DENY, result);
    }

    @Test
    void evaluate_shouldAllowManagementExportArchivedProject() {
        PolicyResult result = policy.evaluate(
                buildSubject(DeptType.MANAGEMENT),
                buildProject(ProjectPhase.ARCHIVED),
                Action.EXPORT,
                environment
        );

        assertEquals(PolicyResult.ALLOW, result);
    }

    @Test
    void evaluate_shouldDenyNonManagementReadArchivedProject() {
        PolicyResult result = policy.evaluate(
                buildSubject(DeptType.QA),
                buildProject(ProjectPhase.ARCHIVED),
                Action.READ,
                environment
        );

        assertEquals(PolicyResult.DENY, result);
    }

    @Test
    void evaluate_shouldDenyWhenPhaseMissing() {
        Resource resource = Resource.builder()
                .type(ResourceType.PROJECT)
                .projectPhase(null)
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

    private Subject buildSubject(DeptType deptType) {
        return new Subject(1L, 1L, deptType, 1L, EmployeeLevel.P6, false);
    }

    private Resource buildProject(ProjectPhase phase) {
        return Resource.builder()
                .type(ResourceType.PROJECT)
                .projectId(11L)
                .resourceId(11L)
                .projectPhase(phase)
                .securityLevel(SecurityLevel.INTERNAL)
                .build();
    }
}
