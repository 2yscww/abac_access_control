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
import com.xie.platform.service.ProjectMemberService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProjectMembershipPolicyTest {

    @Test
    void evaluate_shouldAllowManagementReadWithoutMembershipLookup() {
        ProjectMemberService projectMemberService = mock(ProjectMemberService.class);
        ProjectMembershipPolicy policy = new ProjectMembershipPolicy(projectMemberService);

        PolicyResult result = policy.evaluate(
                new Subject(1L, 10L, DeptType.MANAGEMENT, 1L, EmployeeLevel.VP, false),
                buildProjectResource(),
                Action.READ,
                buildEnvironment()
        );

        assertEquals(PolicyResult.ALLOW, result);
        verifyNoInteractions(projectMemberService);
    }

    @Test
    void evaluate_shouldDenyReadWhenOperatorIsNotActiveMember() {
        ProjectMemberService projectMemberService = mock(ProjectMemberService.class);
        when(projectMemberService.isActiveMember(11L, 1L)).thenReturn(false);
        ProjectMembershipPolicy policy = new ProjectMembershipPolicy(projectMemberService);

        PolicyResult result = policy.evaluate(
                new Subject(1L, 2L, DeptType.RD, 1L, EmployeeLevel.P6, false),
                buildProjectResource(),
                Action.READ,
                buildEnvironment()
        );

        assertEquals(PolicyResult.DENY, result);
    }

    private Resource buildProjectResource() {
        return Resource.builder()
                .type(ResourceType.PROJECT)
                .resourceId(11L)
                .projectId(11L)
                .projectPhase(ProjectPhase.DEVELOPMENT)
                .securityLevel(SecurityLevel.INTERNAL)
                .build();
    }

    private Environment buildEnvironment() {
        return Environment.builder()
                .requestTime(LocalDateTime.of(2026, 3, 19, 10, 0, 0))
                .ipAddress("10.0.0.1")
                .requestUri("/api/project/11")
                .build();
    }
}
