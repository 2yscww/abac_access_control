package com.xie.platform.access.pdp;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.environment.Environment;
import com.xie.platform.access.policy.impl.AssetStageAccessPolicy;
import com.xie.platform.access.policy.impl.EnvironmentAccessPolicy;
import com.xie.platform.access.policy.impl.HistoricalExportPolicy;
import com.xie.platform.access.policy.impl.PhaseAccessPolicy;
import com.xie.platform.access.policy.impl.ProjectMembershipPolicy;
import com.xie.platform.access.policy.impl.ProjectOwnerPhasePolicy;
import com.xie.platform.access.policy.impl.SecurityLevelPolicy;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.access.resource.ResourceType;
import com.xie.platform.access.subject.Subject;
import com.xie.platform.mapper.DepartmentMapper;
import com.xie.platform.model.Department;
import com.xie.platform.model.enumValue.DeptType;
import com.xie.platform.model.enumValue.EmployeeLevel;
import com.xie.platform.model.enumValue.ProjectPhase;
import com.xie.platform.model.enumValue.SecurityLevel;
import com.xie.platform.service.ProjectMemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbacPolicyDecisionPointTest {

    private AbacPolicyDecisionPoint pdp;
    private Environment defaultEnv;
    private ProjectMemberService projectMemberService;

    @BeforeEach
    void setUp() {
        DepartmentMapper departmentMapper = mock(DepartmentMapper.class);
        projectMemberService = mock(ProjectMemberService.class);
        when(departmentMapper.selectById(1L)).thenReturn(buildDepartment(1L, DeptType.RD, 9L));
        when(projectMemberService.isActiveMember(anyLong(), anyLong())).thenReturn(true);

        pdp = new AbacPolicyDecisionPoint();
        pdp.allPolicies = Arrays.asList(
                new SecurityLevelPolicy(),
                new EnvironmentAccessPolicy(),
                new HistoricalExportPolicy(),
                new ProjectMembershipPolicy(projectMemberService),
                new PhaseAccessPolicy(),
                new AssetStageAccessPolicy(),
                new ProjectOwnerPhasePolicy(departmentMapper)
        );
        pdp.init();

        defaultEnv = Environment.builder()
                .requestTime(LocalDateTime.of(2026, 3, 19, 9, 0, 0))
                .ipAddress("192.168.1.100")
                .requestUri("/api/test")
                .build();
    }

    @Test
    void securityPolicy_shouldDenyWhenEmployeeLevelIsInsufficient() {
        Subject subject = new Subject(1L, 1L, DeptType.RD, 1L, EmployeeLevel.P4, false);
        Resource resource = Resource.builder()
                .type(ResourceType.PROJECT)
                .projectPhase(ProjectPhase.DEVELOPMENT)
                .securityLevel(SecurityLevel.CONFIDENTIAL)
                .build();

        DecisionResult result = pdp.evaluate(subject, resource, Action.READ, defaultEnv);

        assertFalse(result.isAllowed());
        assertEquals("SecurityLevelPolicy", result.getTriggerPolicy());
    }

    @Test
    void phasePolicy_shouldDenyRdReadingInitProject() {
        Subject subject = new Subject(1L, 1L, DeptType.RD, 1L, EmployeeLevel.P6, false);
        Resource resource = Resource.builder()
                .type(ResourceType.PROJECT)
                .projectId(11L)
                .projectPhase(ProjectPhase.INIT)
                .securityLevel(SecurityLevel.INTERNAL)
                .build();

        DecisionResult result = pdp.evaluate(subject, resource, Action.READ, defaultEnv);

        assertFalse(result.isAllowed());
        assertEquals("PhaseAccessPolicy", result.getTriggerPolicy());
    }

    @Test
    void phasePolicy_shouldAllowManagementReadingDevelopmentProject() {
        Subject subject = new Subject(1L, 1L, DeptType.MANAGEMENT, 1L, EmployeeLevel.VP, false);
        Resource resource = Resource.builder()
                .type(ResourceType.PROJECT)
                .projectId(11L)
                .projectPhase(ProjectPhase.DEVELOPMENT)
                .securityLevel(SecurityLevel.INTERNAL)
                .build();

        DecisionResult result = pdp.evaluate(subject, resource, Action.READ, defaultEnv);

        assertTrue(result.isAllowed());
        assertEquals("default-allow", result.getTriggerPolicy());
    }

    @Test
    void assetStagePolicy_shouldDenyRdReadingHistoricalInitAsset() {
        Subject subject = new Subject(1L, 1L, DeptType.RD, 1L, EmployeeLevel.P6, false);
        Resource resource = Resource.builder()
                .type(ResourceType.ASSET)
                .projectId(11L)
                .projectPhase(ProjectPhase.DEVELOPMENT)
                .assetsStage(ProjectPhase.INIT)
                .securityLevel(SecurityLevel.INTERNAL)
                .build();

        DecisionResult result = pdp.evaluate(subject, resource, Action.READ, defaultEnv);

        assertFalse(result.isAllowed());
        assertEquals("AssetStageAccessPolicy", result.getTriggerPolicy());
    }

    @Test
    void phasePolicy_shouldDenyWhenDeptTypeIsMissing() {
        Subject subject = new Subject(1L, 1L, null, 1L, EmployeeLevel.P6, false);
        Resource resource = Resource.builder()
                .type(ResourceType.PROJECT)
                .projectId(11L)
                .projectPhase(ProjectPhase.DEVELOPMENT)
                .securityLevel(SecurityLevel.INTERNAL)
                .build();

        DecisionResult result = pdp.evaluate(subject, resource, Action.READ, defaultEnv);

        assertFalse(result.isAllowed());
        assertEquals("PhaseAccessPolicy", result.getTriggerPolicy());
    }

    @Test
    void environmentPolicy_shouldDenyHighSecurityReadOutsideWorkingHours() {
        Subject subject = new Subject(1L, 1L, DeptType.RD, 1L, EmployeeLevel.P6, false);
        Resource resource = Resource.builder()
                .type(ResourceType.PROJECT)
                .projectId(11L)
                .projectPhase(ProjectPhase.DEVELOPMENT)
                .securityLevel(SecurityLevel.CONFIDENTIAL)
                .build();
        Environment lateNightEnv = Environment.builder()
                .requestTime(LocalDateTime.of(2026, 3, 19, 22, 0, 0))
                .ipAddress("10.0.0.8")
                .requestUri("/api/project/11")
                .build();

        DecisionResult result = pdp.evaluate(subject, resource, Action.READ, lateNightEnv);

        assertFalse(result.isAllowed());
        assertEquals("EnvironmentAccessPolicy", result.getTriggerPolicy());
    }

    @Test
    void ownerPolicy_shouldDenyPhaseAdvanceWhenOperatorIsNotConfiguredManager() {
        Subject subject = new Subject(1L, 1L, DeptType.RD, 1L, EmployeeLevel.P6, false);
        Resource resource = Resource.builder()
                .type(ResourceType.PROJECT)
                .resourceId(11L)
                .projectId(11L)
                .projectPhase(ProjectPhase.DEVELOPMENT)
                .securityLevel(SecurityLevel.INTERNAL)
                .ownerId(1L)
                .build();

        DecisionResult result = pdp.evaluate(subject, resource, Action.ADVANCE_PHASE, defaultEnv);

        assertFalse(result.isAllowed());
        assertEquals("ProjectOwnerPhasePolicy", result.getTriggerPolicy());
    }

    @Test
    void membershipPolicy_shouldDenyNonMemberProjectRead() {
        when(projectMemberService.isActiveMember(11L, 1L)).thenReturn(false);

        Subject subject = new Subject(1L, 1L, DeptType.RD, 1L, EmployeeLevel.P6, false);
        Resource resource = Resource.builder()
                .type(ResourceType.PROJECT)
                .resourceId(11L)
                .projectId(11L)
                .projectPhase(ProjectPhase.DEVELOPMENT)
                .securityLevel(SecurityLevel.INTERNAL)
                .build();

        DecisionResult result = pdp.evaluate(subject, resource, Action.READ, defaultEnv);

        assertFalse(result.isAllowed());
        assertEquals("ProjectMembershipPolicy", result.getTriggerPolicy());
    }

    private Department buildDepartment(Long deptId, DeptType deptType, Long managerId) {
        Department department = new Department();
        department.setDeptId(deptId);
        department.setDeptName(deptType.name());
        department.setDeptType(deptType);
        department.setManagerId(managerId);
        return department;
    }
}
