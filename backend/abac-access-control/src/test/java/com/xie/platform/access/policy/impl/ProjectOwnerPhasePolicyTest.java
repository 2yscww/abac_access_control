package com.xie.platform.access.policy.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.environment.Environment;
import com.xie.platform.access.policy.PolicyResult;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.access.resource.ResourceType;
import com.xie.platform.access.subject.Subject;
import com.xie.platform.mapper.DepartmentMapper;
import com.xie.platform.model.Department;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectOwnerPhasePolicyTest {

    @Mock
    private DepartmentMapper departmentMapper;

    @InjectMocks
    private ProjectOwnerPhasePolicy policy;

    @Test
    void evaluate_shouldAllowPhaseAdvanceForConfiguredDepartmentManager() {
        when(departmentMapper.selectById(2L)).thenReturn(buildDepartment(2L, DeptType.RD, 7L));

        PolicyResult result = policy.evaluate(
                buildSubject(7L, 2L, DeptType.RD),
                buildProjectResource(ProjectPhase.DEVELOPMENT),
                Action.ADVANCE_PHASE,
                buildEnvironment()
        );

        assertEquals(PolicyResult.ALLOW, result);
    }

    @Test
    void evaluate_shouldDenyPhaseAdvanceForNonManagerInResponsibleDepartment() {
        when(departmentMapper.selectById(2L)).thenReturn(buildDepartment(2L, DeptType.RD, 9L));

        PolicyResult result = policy.evaluate(
                buildSubject(7L, 2L, DeptType.RD),
                buildProjectResource(ProjectPhase.DEVELOPMENT),
                Action.ADVANCE_PHASE,
                buildEnvironment()
        );

        assertEquals(PolicyResult.DENY, result);
    }

    @Test
    void evaluate_shouldDenyBeforeDepartmentLookupWhenDeptTypeDoesNotMatchPhase() {
        PolicyResult result = policy.evaluate(
                buildSubject(7L, 2L, DeptType.PRODUCT),
                buildProjectResource(ProjectPhase.DEVELOPMENT),
                Action.ADVANCE_PHASE,
                buildEnvironment()
        );

        assertEquals(PolicyResult.DENY, result);
        verifyNoInteractions(departmentMapper);
    }

    private Subject buildSubject(Long employeeId, Long deptId, DeptType deptType) {
        return new Subject(employeeId, deptId, deptType, 1L, EmployeeLevel.P6, false);
    }

    private Resource buildProjectResource(ProjectPhase projectPhase) {
        return Resource.builder()
                .type(ResourceType.PROJECT)
                .resourceId(11L)
                .projectId(11L)
                .projectPhase(projectPhase)
                .securityLevel(SecurityLevel.INTERNAL)
                .ownerId(999L)
                .build();
    }

    private Environment buildEnvironment() {
        return Environment.builder()
                .requestTime(LocalDateTime.of(2026, 3, 19, 10, 0, 0))
                .ipAddress("10.0.0.1")
                .requestUri("/api/project/11/phase")
                .build();
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
