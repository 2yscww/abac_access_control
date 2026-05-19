package com.xie.platform.service.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.pep.PolicyEnforcementPoint;
import com.xie.platform.dto.CreateProjectDTO;
import com.xie.platform.dto.PhaseOwnerPreviewDTO;
import com.xie.platform.dto.UpdateProjectPhaseDTO;
import com.xie.platform.exception.BizException;
import com.xie.platform.mapper.DepartmentMapper;
import com.xie.platform.mapper.EmployeesMapper;
import com.xie.platform.mapper.ProjectMapper;
import com.xie.platform.model.Department;
import com.xie.platform.model.Employees;
import com.xie.platform.model.Projects;
import com.xie.platform.model.enumValue.DeptType;
import com.xie.platform.model.enumValue.EmployeeLevel;
import com.xie.platform.model.enumValue.EmployeeStatus;
import com.xie.platform.model.enumValue.ProjectPhase;
import com.xie.platform.model.enumValue.SecurityLevel;
import com.xie.platform.service.AuditLogService;
import com.xie.platform.service.ProjectMemberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private EmployeesMapper employeesMapper;

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private PolicyEnforcementPoint pep;

    @Mock
    private ProjectMemberService projectMemberService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ProjectServiceImpl projectService;

    @Test
    void createProject_shouldInitializeProjectMembers() {
        CreateProjectDTO dto = new CreateProjectDTO();
        dto.setProjectName("Alpha");
        dto.setProjectPhase(ProjectPhase.DEVELOPMENT.getCode());
        dto.setSecurityLevel(SecurityLevel.INTERNAL.getLevel());
        dto.setOwnerId(9L);

        when(employeesMapper.selectByEmployeeId(9L)).thenReturn(buildEmployee(9L, 2L, EmployeeStatus.ACTIVE));
        when(departmentMapper.selectById(2L)).thenReturn(buildDepartment(2L, DeptType.RD, 9L));
        doAnswer(invocation -> {
            Projects project = invocation.getArgument(0);
            project.setProjectId(11L);
            return 1;
        }).when(projectMapper).insert(any(Projects.class));

        projectService.createProject(dto, 7L);

        verify(pep).checkAccess(eq(7L), any(), eq(Action.WRITE));
        verify(projectMemberService).initializeProjectMembers(11L, 7L, 9L, ProjectPhase.DEVELOPMENT);
    }

    @Test
    void updateProjectPhase_shouldSyncProjectMembers() {
        UpdateProjectPhaseDTO dto = new UpdateProjectPhaseDTO();
        dto.setProjectId(11L);
        dto.setNewPhase(ProjectPhase.TEST.getCode());

        Projects project = new Projects();
        project.setProjectId(11L);
        project.setProjectName("Alpha");
        project.setProjectPhase(ProjectPhase.DEVELOPMENT);
        project.setOwnerId(9L);

        when(projectMapper.selectById(11L)).thenReturn(project);
        when(employeesMapper.selectByEmployeeId(12L)).thenReturn(buildEmployee(12L, 3L, EmployeeStatus.ACTIVE));
        when(departmentMapper.selectByDeptType(DeptType.QA)).thenReturn(buildDepartment(3L, DeptType.QA, 12L));

        projectService.updateProjectPhase(dto, 7L);

        verify(pep).checkProjectAccess(7L, 11L, Action.ADVANCE_PHASE);
        verify(projectMapper).updatePhase(11L, ProjectPhase.TEST.getCode(), 12L);
        verify(projectMemberService).syncMembersForPhaseTransition(11L, ProjectPhase.DEVELOPMENT, ProjectPhase.TEST, 12L, 7L);
        verify(auditLogService).recordBusinessEvent(
                eq(7L),
                eq("PROJECT"),
                eq(11L),
                eq(Action.ADVANCE_PHASE),
                any()
        );
    }

    @Test
    void updateProjectPhase_shouldRejectSkippingStages() {
        UpdateProjectPhaseDTO dto = new UpdateProjectPhaseDTO();
        dto.setProjectId(11L);
        dto.setNewPhase(ProjectPhase.RELEASE.getCode());

        Projects project = new Projects();
        project.setProjectId(11L);
        project.setProjectPhase(ProjectPhase.DEVELOPMENT);

        when(projectMapper.selectById(11L)).thenReturn(project);

        BizException exception = assertThrows(
                BizException.class,
                () -> projectService.updateProjectPhase(dto, 7L)
        );

        assertEquals("项目阶段只能按既定顺序推进到下一阶段", exception.getMessage());
        verify(pep).checkProjectAccess(7L, 11L, Action.ADVANCE_PHASE);
        verify(projectMapper, never()).updatePhase(any(), any(), any());
        verify(projectMemberService, never()).syncMembersForPhaseTransition(any(), any(), any(), any(), any());
    }

    @Test
    void updateProjectPhase_shouldRejectRollback() {
        UpdateProjectPhaseDTO dto = new UpdateProjectPhaseDTO();
        dto.setProjectId(11L);
        dto.setNewPhase(ProjectPhase.REQUIREMENT.getCode());

        Projects project = new Projects();
        project.setProjectId(11L);
        project.setProjectPhase(ProjectPhase.DEVELOPMENT);

        when(projectMapper.selectById(11L)).thenReturn(project);

        BizException exception = assertThrows(
                BizException.class,
                () -> projectService.updateProjectPhase(dto, 7L)
        );

        assertEquals("项目阶段只能按既定顺序推进到下一阶段", exception.getMessage());
        verify(pep).checkProjectAccess(7L, 11L, Action.ADVANCE_PHASE);
        verify(projectMapper, never()).updatePhase(any(), any(), any());
        verify(projectMemberService, never()).syncMembersForPhaseTransition(any(), any(), any(), any(), any());
    }

    @Test
    void deleteProject_shouldDeleteMembershipsBeforeProjectRecord() {
        Projects project = new Projects();
        project.setProjectId(11L);
        project.setProjectPhase(ProjectPhase.DEVELOPMENT);

        when(projectMapper.selectById(11L)).thenReturn(project);

        projectService.deleteProject(11L, 7L);

        verify(pep).checkProjectAccess(7L, 11L, Action.DELETE);
        verify(projectMemberService).deleteByProjectId(11L);
        verify(projectMapper).deleteById(11L);
    }

    @Test
    void getPhaseOwnerPreview_shouldReturnConfiguredOwner() {
        Projects project = new Projects();
        project.setProjectId(11L);
        project.setProjectPhase(ProjectPhase.DEVELOPMENT);

        when(projectMapper.selectById(11L)).thenReturn(project);
        when(departmentMapper.selectByDeptType(DeptType.QA)).thenReturn(buildDepartment(3L, DeptType.QA, 12L));
        when(employeesMapper.selectByEmployeeId(12L)).thenReturn(buildEmployee(12L, 3L, EmployeeStatus.ACTIVE));

        PhaseOwnerPreviewDTO result =
                projectService.getPhaseOwnerPreview(11L, ProjectPhase.TEST.getCode(), 7L);

        assertEquals(12L, result.getEmployeeId());
        assertEquals("测试验证", result.getTargetPhaseDesc());
        assertEquals(Boolean.TRUE, result.getConfigured());
        verify(pep).checkProjectAccess(7L, 11L, Action.ADVANCE_PHASE);
    }

    @Test
    void createProject_shouldRejectDuplicateProjectName() {
        CreateProjectDTO dto = new CreateProjectDTO();
        dto.setProjectName("Alpha");
        dto.setProjectPhase(ProjectPhase.DEVELOPMENT.getCode());
        dto.setSecurityLevel(SecurityLevel.INTERNAL.getLevel());
        dto.setOwnerId(9L);

        Projects existing = new Projects();
        existing.setProjectId(1L);
        existing.setProjectName("Alpha");

        when(projectMapper.selectByName("Alpha")).thenReturn(existing);

        BizException exception = assertThrows(
                BizException.class,
                () -> projectService.createProject(dto, 7L)
        );

        assertEquals("项目名称已存在", exception.getMessage());
        verify(pep, never()).checkAccess(any(), any(), any());
    }

    @Test
    void getPhaseOwnerPreview_shouldRejectArchivedProject() {
        Projects project = new Projects();
        project.setProjectId(11L);
        project.setProjectPhase(ProjectPhase.ARCHIVED);

        when(projectMapper.selectById(11L)).thenReturn(project);

        BizException exception = assertThrows(
                BizException.class,
                () -> projectService.getPhaseOwnerPreview(11L, ProjectPhase.ARCHIVED.getCode(), 7L)
        );

        assertEquals("已归档项目不能修改阶段", exception.getMessage());
        verify(pep).checkProjectAccess(7L, 11L, Action.ADVANCE_PHASE);
    }

    private Employees buildEmployee(Long employeeId, Long deptId, EmployeeStatus status) {
        Employees employee = new Employees();
        employee.setEmployeeId(employeeId);
        employee.setDeptId(deptId);
        employee.setStatus(status);
        employee.setLevel(EmployeeLevel.P6);
        return employee;
    }

    private Department buildDepartment(Long deptId, DeptType deptType, Long managerId) {
        Department department = new Department();
        department.setDeptId(deptId);
        department.setDeptType(deptType);
        department.setManagerId(managerId);
        return department;
    }
}
