package com.xie.platform.service.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.dto.CreateEmployeeDTO;
import com.xie.platform.dto.EmployeeProfileDTO;
import com.xie.platform.dto.OffboardEmployeeDTO;
import com.xie.platform.exception.BizException;
import com.xie.platform.mapper.BranchMapper;
import com.xie.platform.mapper.DepartmentMapper;
import com.xie.platform.mapper.EmployeesMapper;
import com.xie.platform.model.Branch;
import com.xie.platform.model.Department;
import com.xie.platform.model.Employees;
import com.xie.platform.model.enumValue.DeptType;
import com.xie.platform.model.enumValue.EmployeeLevel;
import com.xie.platform.model.enumValue.EmployeeStatus;
import com.xie.platform.service.AuditLogService;
import com.xie.platform.service.PolicyConfigService;
import com.xie.platform.service.ProjectMemberService;
import com.xie.platform.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeAuthServiceImplTest {

    @Mock
    private EmployeesMapper employeesMapper;

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private BranchMapper branchMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ProjectMemberService projectMemberService;

    @Mock
    private PolicyConfigService policyConfigService;

    @InjectMocks
    private EmployeeAuthServiceImpl employeeAuthService;

    @Test
    void createEmployee_shouldRejectNonHrOperator() {
        CreateEmployeeDTO dto = buildCreateEmployeeDto();

        Employees operator = buildOperator(10L, 1L, EmployeeStatus.ACTIVE);
        Department operatorDept = new Department();
        operatorDept.setDeptId(1L);
        operatorDept.setDeptType(DeptType.RD);

        when(employeesMapper.selectByEmployeeId(10L)).thenReturn(operator);
        when(departmentMapper.selectById(1L)).thenReturn(operatorDept);

        BizException exception = assertThrows(
                BizException.class,
                () -> employeeAuthService.createEmployee(dto, 10L)
        );

        assertEquals("仅人事部允许执行该操作", exception.getMessage());
        verify(employeesMapper, never()).insert(any(Employees.class));
        verify(employeesMapper, never()).updateEmployeeCode(any(), any());
    }

    @Test
    void createEmployee_shouldCreateEmployeeForHrOperator() {
        CreateEmployeeDTO dto = buildCreateEmployeeDto();

        Employees operator = buildOperator(10L, 1L, EmployeeStatus.ACTIVE);
        Department operatorDept = new Department();
        operatorDept.setDeptId(1L);
        operatorDept.setDeptType(DeptType.HR);

        Department targetDept = new Department();
        targetDept.setDeptId(dto.getDeptId());
        targetDept.setDeptType(DeptType.RD);

        Branch branch = new Branch();
        branch.setBranchId(dto.getBranchId());

        when(employeesMapper.selectByEmployeeId(10L)).thenReturn(operator);
        when(departmentMapper.selectById(1L)).thenReturn(operatorDept);
        when(departmentMapper.selectById(dto.getDeptId())).thenReturn(targetDept);
        when(branchMapper.selectById(dto.getBranchId())).thenReturn(branch);
        when(passwordEncoder.encode("ABACtest")).thenReturn("ENCODED_DEFAULT_PASSWORD");
        doAnswer(invocation -> {
            Employees employee = invocation.getArgument(0);
            employee.setEmployeeId(5L);
            return 1;
        }).when(employeesMapper).insert(any(Employees.class));

        employeeAuthService.createEmployee(dto, 10L);

        ArgumentCaptor<Employees> employeeCaptor = ArgumentCaptor.forClass(Employees.class);
        verify(employeesMapper).insert(employeeCaptor.capture());
        verify(employeesMapper).updateEmployeeCode(5L, "1005");
        verify(passwordEncoder).encode("ABACtest");

        Employees createdEmployee = employeeCaptor.getValue();
        assertEquals("PENDING", createdEmployee.getEmployeeCode());
        assertEquals(dto.getEmployeeName(), createdEmployee.getEmployeeName());
        assertEquals(dto.getDeptId(), createdEmployee.getDeptId());
        assertEquals(dto.getBranchId(), createdEmployee.getBranchId());
        assertEquals(EmployeeLevel.P4, createdEmployee.getLevel());
        assertFalse(createdEmployee.getIsContractor());
        assertEquals(EmployeeStatus.ACTIVE, createdEmployee.getStatus());
        assertEquals("ENCODED_DEFAULT_PASSWORD", createdEmployee.getPassword());
        assertTrue(createdEmployee.getMustChangePassword());
    }

    @Test
    void offboardEmployee_shouldRejectNonHrOperator() {
        Employees operator = buildOperator(10L, 1L, EmployeeStatus.ACTIVE);
        Department operatorDept = new Department();
        operatorDept.setDeptId(1L);
        operatorDept.setDeptType(DeptType.RD);

        when(employeesMapper.selectByEmployeeId(10L)).thenReturn(operator);
        when(departmentMapper.selectById(1L)).thenReturn(operatorDept);

        OffboardEmployeeDTO dto = new OffboardEmployeeDTO();
        dto.setEmployeeId(5L);

        BizException exception = assertThrows(
                BizException.class,
                () -> employeeAuthService.offboardEmployee(dto, 10L)
        );

        assertEquals("仅人事部允许执行该操作", exception.getMessage());
        verify(employeesMapper, never()).updateStatus(any(), any());
    }

    @Test
    void offboardEmployee_shouldMarkEmployeeInactiveAndAudit() {
        Employees operator = buildOperator(10L, 1L, EmployeeStatus.ACTIVE);
        Department operatorDept = new Department();
        operatorDept.setDeptId(1L);
        operatorDept.setDeptType(DeptType.HR);

        Employees target = buildOperator(5L, 2L, EmployeeStatus.ACTIVE);
        target.setEmployeeCode("1005");
        target.setEmployeeName("Alice");

        Department managedDepartment = new Department();
        managedDepartment.setDeptId(2L);
        managedDepartment.setDeptType(DeptType.RD);

        when(employeesMapper.selectByEmployeeId(10L)).thenReturn(operator);
        when(departmentMapper.selectById(1L)).thenReturn(operatorDept);
        when(employeesMapper.selectByEmployeeId(5L)).thenReturn(target);
        when(departmentMapper.selectByManagerId(5L)).thenReturn(List.of(managedDepartment));

        OffboardEmployeeDTO dto = new OffboardEmployeeDTO();
        dto.setEmployeeId(5L);

        employeeAuthService.offboardEmployee(dto, 10L);

        verify(employeesMapper).updateStatus(5L, EmployeeStatus.INACTIVE);
        verify(projectMemberService).deactivateByEmployeeId(5L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> detailCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditLogService).recordBusinessEvent(
                eq(10L),
                eq("EMPLOYEE"),
                eq(5L),
                eq(Action.OFFBOARD_EMPLOYEE),
                detailCaptor.capture()
        );

        Map<String, Object> detail = detailCaptor.getValue();
        assertEquals(10L, detail.get("approverId"));
        assertEquals(5L, detail.get("offboardEmployeeId"));
        assertEquals("1005", detail.get("offboardEmployeeCode"));
        assertEquals("Alice", detail.get("offboardEmployeeName"));
        assertEquals(List.of(2L), detail.get("managedDeptIds"));
        assertEquals(List.of("RD"), detail.get("managedDeptTypes"));
    }

    @Test
    void getCurrentEmployeeProfile_shouldExposeManagementAuditAndAssignCapabilities() {
        Employees employee = buildOperator(20L, 1L, EmployeeStatus.ACTIVE);
        employee.setEmployeeCode("1020");
        employee.setEmployeeName("Manager");
        employee.setBranchId(3L);
        employee.setLevel(EmployeeLevel.VP);

        when(employeesMapper.selectByEmployeeId(20L)).thenReturn(employee);
        when(departmentMapper.selectById(1L)).thenReturn(buildDepartment(1L, DeptType.MANAGEMENT));
        when(policyConfigService.isPolicyAdmin(20L)).thenReturn(true);

        EmployeeProfileDTO profile = employeeAuthService.getCurrentEmployeeProfile(20L);

        assertEquals(List.of("projects", "assets", "files", "handover", "audit", "policy"), profile.getVisibleMenus());
        assertEquals(
                List.of(
                        "projects.view",
                        "assets.view",
                        "files.view",
                        "files.upload",
                        "files.download",
                        "handover.view",
                        "handover.assign",
                        "audit.view",
                        "policy.manage"
                ),
                profile.getCapabilities()
        );
    }

    @Test
    void getCurrentEmployeeProfile_shouldExposeHrOffboardCapabilityWithoutAudit() {
        Employees employee = buildOperator(30L, 2L, EmployeeStatus.ACTIVE);
        employee.setEmployeeCode("1030");
        employee.setEmployeeName("Hr");
        employee.setBranchId(3L);
        employee.setLevel(EmployeeLevel.P6);

        when(employeesMapper.selectByEmployeeId(30L)).thenReturn(employee);
        when(departmentMapper.selectById(2L)).thenReturn(buildDepartment(2L, DeptType.HR));

        EmployeeProfileDTO profile = employeeAuthService.getCurrentEmployeeProfile(30L);

        assertEquals(List.of("projects", "assets", "files", "handover"), profile.getVisibleMenus());
        assertEquals(
                List.of(
                        "projects.view",
                        "assets.view",
                        "files.view",
                        "files.upload",
                        "files.download",
                        "handover.view",
                        "handover.offboard"
                ),
                profile.getCapabilities()
        );
    }

    @Test
    void getCurrentEmployeeProfile_shouldHideAuditAndHandoverFromGeneralStaff() {
        Employees employee = buildOperator(40L, 3L, EmployeeStatus.ACTIVE);
        employee.setEmployeeCode("1040");
        employee.setEmployeeName("Engineer");
        employee.setBranchId(3L);
        employee.setLevel(EmployeeLevel.P6);

        when(employeesMapper.selectByEmployeeId(40L)).thenReturn(employee);
        when(departmentMapper.selectById(3L)).thenReturn(buildDepartment(3L, DeptType.RD));

        EmployeeProfileDTO profile = employeeAuthService.getCurrentEmployeeProfile(40L);

        assertEquals(List.of("projects", "assets", "files"), profile.getVisibleMenus());
        assertEquals(
                List.of(
                        "projects.view",
                        "assets.view",
                        "files.view",
                        "files.upload",
                        "files.download"
                ),
                profile.getCapabilities()
        );
    }

    private CreateEmployeeDTO buildCreateEmployeeDto() {
        CreateEmployeeDTO dto = new CreateEmployeeDTO();
        dto.setEmployeeName("Alice");
        dto.setDeptId(2L);
        dto.setBranchId(3L);
        dto.setLevel(EmployeeLevel.P4.getRank());
        dto.setIsContractor(false);
        return dto;
    }

    private Employees buildOperator(Long employeeId, Long deptId, EmployeeStatus status) {
        Employees operator = new Employees();
        operator.setEmployeeId(employeeId);
        operator.setDeptId(deptId);
        operator.setStatus(status);
        return operator;
    }

    private Department buildDepartment(Long deptId, DeptType deptType) {
        Department department = new Department();
        department.setDeptId(deptId);
        department.setDeptType(deptType);
        return department;
    }
}
