package com.xie.platform.service.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.dto.ProjectMemberDTO;
import com.xie.platform.exception.BizException;
import com.xie.platform.mapper.DepartmentMapper;
import com.xie.platform.mapper.EmployeesMapper;
import com.xie.platform.mapper.ProjectMapper;
import com.xie.platform.mapper.ProjectMemberMapper;
import com.xie.platform.model.Department;
import com.xie.platform.model.Employees;
import com.xie.platform.model.ProjectMember;
import com.xie.platform.model.Projects;
import com.xie.platform.model.enumValue.DeptType;
import com.xie.platform.model.enumValue.EmployeeStatus;
import com.xie.platform.model.enumValue.ProjectMemberStatus;
import com.xie.platform.model.enumValue.ProjectPhase;
import com.xie.platform.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceImplTest {

    @Mock
    private ProjectMemberMapper projectMemberMapper;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private EmployeesMapper employeesMapper;

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ProjectMemberServiceImpl projectMemberService;

    @Test
    void syncMembersForPhaseTransition_shouldDeactivateDisallowedMembersAndAddNextOwner() {
        ProjectMemberDTO productMember = buildMemberDto(7L, DeptType.PRODUCT);
        ProjectMemberDTO rdMember = buildMemberDto(8L, DeptType.RD);

        when(projectMemberMapper.selectActiveByProjectId(11L)).thenReturn(List.of(productMember, rdMember));
        when(projectMemberMapper.selectByProjectIdAndEmployeeId(11L, 12L)).thenReturn(null);

        projectMemberService.syncMembersForPhaseTransition(11L, ProjectPhase.TEST, 12L);

        verify(projectMemberMapper).deactivate(11L, 7L);
        verify(projectMemberMapper, never()).deactivate(11L, 8L);

        ArgumentCaptor<ProjectMember> memberCaptor = ArgumentCaptor.forClass(ProjectMember.class);
        verify(projectMemberMapper).insert(memberCaptor.capture());
        assertEquals(11L, memberCaptor.getValue().getProjectId());
        assertEquals(12L, memberCaptor.getValue().getEmployeeId());
        assertEquals(ProjectPhase.TEST, memberCaptor.getValue().getJoinedPhase());
        assertEquals(ProjectMemberStatus.ACTIVE, memberCaptor.getValue().getStatus());
    }

    @Test
    void listProjectMembers_shouldAllowManagementOperator() {
        Projects project = new Projects();
        project.setProjectId(11L);
        project.setOwnerId(9L);

        Employees operator = buildEmployee(20L, 1L, EmployeeStatus.ACTIVE, "1020", "Management");
        Department managementDepartment = buildDepartment(1L, DeptType.MANAGEMENT, 20L);
        List<ProjectMemberDTO> expected = List.of(buildMemberDto(7L, DeptType.RD));

        when(projectMapper.selectById(11L)).thenReturn(project);
        when(employeesMapper.selectByEmployeeId(20L)).thenReturn(operator);
        when(departmentMapper.selectById(1L)).thenReturn(managementDepartment);
        when(projectMemberMapper.selectByProjectId(11L)).thenReturn(expected);

        List<ProjectMemberDTO> result = projectMemberService.listProjectMembers(11L, 20L);

        assertEquals(expected, result);
        verify(projectMemberMapper).selectByProjectId(11L);
    }

    @Test
    void listProjectMembers_shouldRejectNonOwnerNonManagementOperator() {
        Projects project = new Projects();
        project.setProjectId(11L);
        project.setOwnerId(9L);

        Employees operator = buildEmployee(20L, 2L, EmployeeStatus.ACTIVE, "1020", "Engineer");
        Department rdDepartment = buildDepartment(2L, DeptType.RD, 9L);

        when(projectMapper.selectById(11L)).thenReturn(project);
        when(employeesMapper.selectByEmployeeId(20L)).thenReturn(operator);
        when(departmentMapper.selectById(2L)).thenReturn(rdDepartment);

        BizException exception = assertThrows(
                BizException.class,
                () -> projectMemberService.listProjectMembers(11L, 20L)
        );

        assertEquals("仅当前阶段负责人或管理层允许维护项目成员", exception.getMessage());
        verify(projectMemberMapper, never()).selectByProjectId(11L);
    }

    @Test
    void addProjectMember_shouldAllowOwnerToAddAllowedDepartmentMemberAndAudit() {
        Projects project = new Projects();
        project.setProjectId(11L);
        project.setProjectName("Alpha");
        project.setProjectPhase(ProjectPhase.DEVELOPMENT);
        project.setOwnerId(9L);

        Employees operator = buildEmployee(9L, 2L, EmployeeStatus.ACTIVE, "1009", "Owner");
        Employees target = buildEmployee(7L, 2L, EmployeeStatus.ACTIVE, "1007", "Member");
        Department rdDepartment = buildDepartment(2L, DeptType.RD, 9L);

        when(projectMapper.selectById(11L)).thenReturn(project);
        when(employeesMapper.selectByEmployeeId(9L)).thenReturn(operator);
        when(employeesMapper.selectByEmployeeId(7L)).thenReturn(target);
        when(departmentMapper.selectById(2L)).thenReturn(rdDepartment);
        when(projectMemberMapper.countActiveMember(11L, 7L)).thenReturn(0);
        when(projectMemberMapper.selectByProjectIdAndEmployeeId(11L, 7L)).thenReturn(null);
        doAnswer(invocation -> 1).when(projectMemberMapper).insert(any(ProjectMember.class));

        projectMemberService.addProjectMember(11L, 7L, 9L);

        verify(projectMemberMapper).insert(any(ProjectMember.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> detailCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditLogService).recordBusinessEvent(
                eq(9L),
                eq("PROJECT"),
                eq(11L),
                eq(Action.ADD_PROJECT_MEMBER),
                detailCaptor.capture()
        );

        Map<String, Object> detail = detailCaptor.getValue();
        assertEquals(7L, detail.get("targetEmployeeId"));
        assertEquals("1007", detail.get("targetEmployeeCode"));
        assertEquals("RD", detail.get("targetDeptType"));
    }

    @Test
    void removeProjectMember_shouldRejectRemovingCurrentOwner() {
        Projects project = new Projects();
        project.setProjectId(11L);
        project.setProjectName("Alpha");
        project.setProjectPhase(ProjectPhase.DEVELOPMENT);
        project.setOwnerId(9L);

        Employees operator = buildEmployee(9L, 2L, EmployeeStatus.ACTIVE, "1009", "Owner");
        Department rdDepartment = buildDepartment(2L, DeptType.RD, 9L);

        when(projectMapper.selectById(11L)).thenReturn(project);
        when(employeesMapper.selectByEmployeeId(9L)).thenReturn(operator);
        when(departmentMapper.selectById(2L)).thenReturn(rdDepartment);

        BizException exception = assertThrows(
                BizException.class,
                () -> projectMemberService.removeProjectMember(11L, 9L, 9L)
        );

        assertEquals("当前阶段负责人不能直接移出项目成员", exception.getMessage());
        verify(projectMemberMapper, never()).deactivate(any(), any());
    }

    private ProjectMemberDTO buildMemberDto(Long employeeId, DeptType deptType) {
        ProjectMemberDTO dto = new ProjectMemberDTO();
        dto.setProjectId(11L);
        dto.setEmployeeId(employeeId);
        dto.setDeptType(deptType);
        dto.setStatus(ProjectMemberStatus.ACTIVE);
        return dto;
    }

    private Employees buildEmployee(
            Long employeeId,
            Long deptId,
            EmployeeStatus status,
            String employeeCode,
            String employeeName) {
        Employees employee = new Employees();
        employee.setEmployeeId(employeeId);
        employee.setDeptId(deptId);
        employee.setStatus(status);
        employee.setEmployeeCode(employeeCode);
        employee.setEmployeeName(employeeName);
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
