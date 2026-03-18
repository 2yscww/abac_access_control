package com.xie.platform.service.impl;

import com.xie.platform.dto.CreateEmployeeDTO;
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
import com.xie.platform.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

        assertEquals("仅人事部允许创建员工", exception.getMessage());
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
}
