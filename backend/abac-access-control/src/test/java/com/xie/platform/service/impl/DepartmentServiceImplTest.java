package com.xie.platform.service.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.dto.AssignDepartmentManagerDTO;
import com.xie.platform.dto.DepartmentManagerHandoverTodoDTO;
import com.xie.platform.dto.EmployeeOptionDTO;
import com.xie.platform.exception.BizException;
import com.xie.platform.mapper.DepartmentMapper;
import com.xie.platform.mapper.EmployeesMapper;
import com.xie.platform.mapper.ProjectMapper;
import com.xie.platform.model.Department;
import com.xie.platform.model.Employees;
import com.xie.platform.model.Projects;
import com.xie.platform.model.enumValue.DeptType;
import com.xie.platform.model.enumValue.EmployeeStatus;
import com.xie.platform.model.enumValue.ProjectPhase;
import com.xie.platform.service.AuditLogService;
import com.xie.platform.service.ProjectMemberService;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private EmployeesMapper employeesMapper;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ProjectMemberService projectMemberService;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    @Test
    void assignDepartmentManager_shouldRejectNonManagementOperator() {
        Employees operator = buildEmployee(1L, 2L, EmployeeStatus.ACTIVE, null, "Operator");
        Department operatorDept = new Department();
        operatorDept.setDeptId(2L);
        operatorDept.setDeptType(DeptType.RD);

        when(employeesMapper.selectByEmployeeId(1L)).thenReturn(operator);
        when(departmentMapper.selectById(2L)).thenReturn(operatorDept);

        AssignDepartmentManagerDTO dto = new AssignDepartmentManagerDTO();
        dto.setDeptId(3L);
        dto.setNewManagerEmployeeId(9L);

        BizException exception = assertThrows(
                BizException.class,
                () -> departmentService.assignDepartmentManager(dto, 1L)
        );

        assertEquals("仅管理层允许执行该操作", exception.getMessage());
        verifyNoInteractions(projectMapper, auditLogService);
        verify(departmentMapper, never()).updateManagerId(any(), any());
    }

    @Test
    void assignDepartmentManager_shouldSyncPhaseOwnersAndAudit() {
        Employees operator = buildEmployee(1L, 10L, EmployeeStatus.ACTIVE, "1001", "Approver");
        Department operatorDept = new Department();
        operatorDept.setDeptId(10L);
        operatorDept.setDeptType(DeptType.MANAGEMENT);

        Department targetDept = new Department();
        targetDept.setDeptId(3L);
        targetDept.setDeptName("研发部");
        targetDept.setDeptType(DeptType.RD);
        targetDept.setManagerId(8L);

        Employees oldManager = buildEmployee(8L, 3L, EmployeeStatus.INACTIVE, "1008", "Old");
        Employees newManager = buildEmployee(9L, 3L, EmployeeStatus.ACTIVE, "1009", "New");

        Projects p1 = new Projects();
        p1.setProjectId(101L);
        p1.setProjectPhase(ProjectPhase.DEVELOPMENT);
        Projects p2 = new Projects();
        p2.setProjectId(102L);
        p2.setProjectPhase(ProjectPhase.DEVELOPMENT);

        when(employeesMapper.selectByEmployeeId(1L)).thenReturn(operator);
        when(departmentMapper.selectById(10L)).thenReturn(operatorDept);
        when(departmentMapper.selectById(3L)).thenReturn(targetDept);
        when(employeesMapper.selectByEmployeeId(9L)).thenReturn(newManager);
        when(employeesMapper.selectByEmployeeId(8L)).thenReturn(oldManager);
        when(projectMapper.selectByPhaseCodes(List.of(ProjectPhase.DEVELOPMENT.getCode())))
                .thenReturn(List.of(p1, p2));

        AssignDepartmentManagerDTO dto = new AssignDepartmentManagerDTO();
        dto.setDeptId(3L);
        dto.setNewManagerEmployeeId(9L);

        departmentService.assignDepartmentManager(dto, 1L);

        verify(departmentMapper).updateManagerId(3L, 9L);
        verify(projectMapper).updateOwnerByPhaseCodes(List.of(ProjectPhase.DEVELOPMENT.getCode()), 9L);
        verify(projectMemberService).ensureProjectOwnerMembership(101L, 9L, ProjectPhase.DEVELOPMENT);
        verify(projectMemberService).ensureProjectOwnerMembership(102L, 9L, ProjectPhase.DEVELOPMENT);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> detailCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditLogService).recordBusinessEvent(
                eq(1L),
                eq("DEPARTMENT"),
                eq(3L),
                eq(Action.ASSIGN_DEPARTMENT_MANAGER),
                detailCaptor.capture()
        );

        Map<String, Object> detail = detailCaptor.getValue();
        assertEquals(1L, detail.get("approverId"));
        assertEquals(3L, detail.get("deptId"));
        assertEquals("研发部", detail.get("deptName"));
        assertEquals("RD", detail.get("deptType"));
        assertEquals(8L, detail.get("oldManagerId"));
        assertEquals(9L, detail.get("newManagerId"));
        assertEquals(List.of(ProjectPhase.DEVELOPMENT.getCode()), detail.get("ownerSyncPhaseCodes"));
        assertEquals(List.of(101L, 102L), detail.get("affectedProjectIds"));
        assertEquals(2, detail.get("affectedProjectCount"));
    }

    @Test
    void assignDepartmentManager_shouldRejectContractorCandidate() {
        Employees operator = buildEmployee(1L, 10L, EmployeeStatus.ACTIVE, "1001", "Approver");
        Department operatorDept = new Department();
        operatorDept.setDeptId(10L);
        operatorDept.setDeptType(DeptType.MANAGEMENT);

        Department targetDept = new Department();
        targetDept.setDeptId(3L);
        targetDept.setDeptName("研发部");
        targetDept.setDeptType(DeptType.RD);
        targetDept.setManagerId(8L);

        Employees contractor = buildEmployee(9L, 3L, EmployeeStatus.ACTIVE, "1009", "Contractor");
        contractor.setIsContractor(true);

        when(employeesMapper.selectByEmployeeId(1L)).thenReturn(operator);
        when(departmentMapper.selectById(10L)).thenReturn(operatorDept);
        when(departmentMapper.selectById(3L)).thenReturn(targetDept);
        when(employeesMapper.selectByEmployeeId(9L)).thenReturn(contractor);

        AssignDepartmentManagerDTO dto = new AssignDepartmentManagerDTO();
        dto.setDeptId(3L);
        dto.setNewManagerEmployeeId(9L);

        BizException exception = assertThrows(
                BizException.class,
                () -> departmentService.assignDepartmentManager(dto, 1L)
        );

        assertEquals("外包员工不能担任部门负责人", exception.getMessage());
        verify(departmentMapper, never()).updateManagerId(any(), any());
        verifyNoInteractions(projectMapper, auditLogService, projectMemberService);
    }

    @Test
    void queryManagerCandidates_shouldExcludeContractors() {
        Employees operator = buildEmployee(1L, 10L, EmployeeStatus.ACTIVE, "1001", "Approver");
        Department operatorDept = new Department();
        operatorDept.setDeptId(10L);
        operatorDept.setDeptType(DeptType.MANAGEMENT);

        Department targetDept = new Department();
        targetDept.setDeptId(3L);
        targetDept.setDeptType(DeptType.RD);

        EmployeeOptionDTO regular = buildEmployeeOption(9L, "1009", "Regular", 3L, false);
        EmployeeOptionDTO contractor = buildEmployeeOption(10L, "1010", "Contractor", 3L, true);

        when(employeesMapper.selectByEmployeeId(1L)).thenReturn(operator);
        when(departmentMapper.selectById(10L)).thenReturn(operatorDept);
        when(departmentMapper.selectById(3L)).thenReturn(targetDept);
        when(employeesMapper.selectActiveOptionsByDeptId(3L)).thenReturn(List.of(regular, contractor));

        List<EmployeeOptionDTO> result = departmentService.queryManagerCandidates(3L, 1L);

        assertEquals(1, result.size());
        assertEquals(9L, result.get(0).getEmployeeId());
        assertTrue(result.stream().noneMatch(item -> Boolean.TRUE.equals(item.getIsContractor())));
    }

    @Test
    void queryManagerHandoverTodos_shouldAttachAffectedProjects() {
        Employees operator = buildEmployee(1L, 10L, EmployeeStatus.ACTIVE, "1001", "Approver");
        Department operatorDept = new Department();
        operatorDept.setDeptId(10L);
        operatorDept.setDeptType(DeptType.MANAGEMENT);

        DepartmentManagerHandoverTodoDTO todo = new DepartmentManagerHandoverTodoDTO();
        todo.setDeptId(3L);
        todo.setDeptName("研发部");
        todo.setDeptType(DeptType.RD);
        todo.setManagerId(8L);
        todo.setManagerCode("1008");
        todo.setManagerName("Old");

        Projects project = new Projects();
        project.setProjectId(101L);
        project.setProjectPhase(ProjectPhase.DEVELOPMENT);

        when(employeesMapper.selectByEmployeeId(1L)).thenReturn(operator);
        when(departmentMapper.selectById(10L)).thenReturn(operatorDept);
        when(departmentMapper.selectWithInactiveManager()).thenReturn(List.of(todo));
        when(projectMapper.selectByPhaseCodes(List.of(ProjectPhase.DEVELOPMENT.getCode())))
                .thenReturn(List.of(project));

        List<DepartmentManagerHandoverTodoDTO> result = departmentService.queryManagerHandoverTodos(1L);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getAffectedProjectCount());
        assertEquals(List.of(project), result.get(0).getAffectedProjects());
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

    private EmployeeOptionDTO buildEmployeeOption(
            Long employeeId,
            String employeeCode,
            String employeeName,
            Long deptId,
            boolean isContractor) {
        EmployeeOptionDTO option = new EmployeeOptionDTO();
        option.setEmployeeId(employeeId);
        option.setEmployeeCode(employeeCode);
        option.setEmployeeName(employeeName);
        option.setDeptId(deptId);
        option.setIsContractor(isContractor);
        option.setStatus(EmployeeStatus.ACTIVE);
        return option;
    }
}
