package com.xie.platform.service.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.subject.Subject;
import com.xie.platform.dto.CreateEmployeeDTO;
import com.xie.platform.dto.EmployeeActiveQueryDTO;
import com.xie.platform.dto.EmployeeOptionDTO;
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
import com.xie.platform.service.EmployeeAuthService;
import com.xie.platform.service.result.LoginResult;
import com.xie.platform.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmployeeAuthServiceImpl implements EmployeeAuthService {

    @Autowired
    private EmployeesMapper employeesMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private BranchMapper branchMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuditLogService auditLogService;

    @Override
    public LoginResult login(String employeeCode, String rawPassword) {
        LoginResult result = new LoginResult();
        Employees employee = employeesMapper.selectByEmployeeCode(employeeCode);

        if (employee == null) {
            result.setSuccess(false);
            result.setMessage("员工不存在");
            return result;
        }
        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            result.setSuccess(false);
            result.setMessage("员工状态不可用");
            return result;
        }
        if (!passwordEncoder.matches(rawPassword, employee.getPassword())) {
            result.setSuccess(false);
            result.setMessage("密码错误");
            return result;
        }

        result.setSuccess(true);
        result.setEmployeeId(employee.getEmployeeId());
        result.setMustChangePassword(Boolean.TRUE.equals(employee.getMustChangePassword()));
        result.setMessage("登录成功");

        if (Boolean.TRUE.equals(employee.getMustChangePassword())) {
            result.setTempToken(jwtUtil.generateTempToken(employee.getEmployeeId()));
            return result;
        }

        result.setToken(jwtUtil.generateToken(buildSubject(employee)));
        return result;
    }

    @Override
    public String changePassword(String tempToken, String oldPassword, String newPassword) {
        Long employeeId;
        try {
            employeeId = jwtUtil.parseAndValidateTempToken(tempToken);
        } catch (Exception exception) {
            throw new BizException("临时凭证无效或已过期，请重新登录");
        }

        Employees employee = employeesMapper.selectByEmployeeId(employeeId);
        if (employee == null) {
            throw new BizException("员工不存在");
        }
        if (!passwordEncoder.matches(oldPassword, employee.getPassword())) {
            throw new BizException("原密码错误");
        }

        employeesMapper.updatePassword(employeeId, passwordEncoder.encode(newPassword));
        return jwtUtil.generateToken(buildSubject(employee));
    }

    @Override
    @Transactional
    public void createEmployee(CreateEmployeeDTO dto, Long operatorEmployeeId) {
        ensureHrOperator(operatorEmployeeId);

        if (dto.getEmployeeName() == null || dto.getEmployeeName().isBlank()) {
            throw new BizException("员工名称不能为空");
        }
        if (dto.getDeptId() == null) {
            throw new BizException("部门不能为空");
        }
        if (dto.getBranchId() == null) {
            throw new BizException("分公司不能为空");
        }
        if (dto.getLevel() == null) {
            throw new BizException("员工职级不能为空");
        }

        Department dept = departmentMapper.selectById(dto.getDeptId());
        if (dept == null) {
            throw new BizException("部门不存在");
        }

        Branch branch = branchMapper.selectById(dto.getBranchId());
        if (branch == null) {
            throw new BizException("分公司不存在");
        }

        EmployeeLevel level;
        try {
            level = EmployeeLevel.fromRank(dto.getLevel());
        } catch (IllegalArgumentException exception) {
            throw new BizException("非法的员工职级");
        }

        String defaultPwd = "ABACtest";
        String encodedPwd = passwordEncoder.encode(defaultPwd);

        Employees employee = new Employees();
        employee.setEmployeeCode("PENDING");
        employee.setEmployeeName(dto.getEmployeeName());
        employee.setDeptId(dto.getDeptId());
        employee.setBranchId(dto.getBranchId());
        employee.setLevel(level);
        employee.setIsContractor(Boolean.TRUE.equals(dto.getIsContractor()));
        employee.setStatus(EmployeeStatus.ACTIVE);
        employee.setPassword(encodedPwd);
        employee.setMustChangePassword(true);

        employeesMapper.insert(employee);
        employeesMapper.updateEmployeeCode(employee.getEmployeeId(), String.valueOf(employee.getEmployeeId() + 1000));
    }

    @Override
    @Transactional
    public void offboardEmployee(OffboardEmployeeDTO dto, Long operatorEmployeeId) {
        ensureHrOperator(operatorEmployeeId);

        if (dto == null || dto.getEmployeeId() == null) {
            throw new BizException("离职员工ID不能为空");
        }

        Employees targetEmployee = employeesMapper.selectByEmployeeId(dto.getEmployeeId());
        if (targetEmployee == null) {
            throw new BizException("离职员工不存在");
        }
        if (targetEmployee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new BizException("离职员工状态不可用");
        }

        employeesMapper.updateStatus(targetEmployee.getEmployeeId(), EmployeeStatus.INACTIVE);

        List<Department> managedDepartments = departmentMapper.selectByManagerId(targetEmployee.getEmployeeId());
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("approverId", operatorEmployeeId);
        detail.put("offboardEmployeeId", targetEmployee.getEmployeeId());
        detail.put("offboardEmployeeCode", targetEmployee.getEmployeeCode());
        detail.put("offboardEmployeeName", targetEmployee.getEmployeeName());
        detail.put("managedDeptIds", managedDepartments.stream().map(Department::getDeptId).toList());
        detail.put("managedDeptTypes", managedDepartments.stream()
                .map(department -> department.getDeptType() != null ? department.getDeptType().name() : null)
                .toList());

        auditLogService.recordBusinessEvent(
                operatorEmployeeId,
                "EMPLOYEE",
                targetEmployee.getEmployeeId(),
                Action.OFFBOARD_EMPLOYEE,
                detail
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeOptionDTO> queryActiveEmployees(EmployeeActiveQueryDTO query, Long operatorEmployeeId) {
        ensureHrOperator(operatorEmployeeId);

        String keyword = query != null && query.getKeyword() != null
                ? query.getKeyword().trim()
                : null;
        return employeesMapper.selectActiveOptions(keyword);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeProfileDTO getCurrentEmployeeProfile(Long employeeId) {
        Employees employee = employeesMapper.selectByEmployeeId(employeeId);
        if (employee == null) {
            throw new BizException("当前员工不存在");
        }
        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new BizException("当前员工状态不可用");
        }

        Department dept = departmentMapper.selectById(employee.getDeptId());
        if (dept == null) {
            throw new BizException("当前员工所属部门不存在");
        }

        EmployeeProfileDTO profile = new EmployeeProfileDTO();
        profile.setEmployeeId(employee.getEmployeeId());
        profile.setEmployeeCode(employee.getEmployeeCode());
        profile.setEmployeeName(employee.getEmployeeName());
        profile.setDeptId(employee.getDeptId());
        profile.setDeptType(dept.getDeptType() != null ? dept.getDeptType().name() : null);
        profile.setDeptTypeDesc(dept.getDeptType() != null ? dept.getDeptType().getDesc() : null);
        profile.setBranchId(employee.getBranchId());
        profile.setLevel(employee.getLevel() != null ? employee.getLevel().name() : null);
        profile.setLevelRank(employee.getLevel() != null ? employee.getLevel().getRank() : null);
        profile.setIsContractor(employee.getIsContractor());
        profile.setStatus(employee.getStatus() != null ? employee.getStatus().name() : null);
        profile.setVisibleMenus(buildVisibleMenus(dept.getDeptType()));
        profile.setCapabilities(buildCapabilities(dept.getDeptType()));
        return profile;
    }

    private Subject buildSubject(Employees employee) {
        Department dept = departmentMapper.selectById(employee.getDeptId());
        return new Subject(
                employee.getEmployeeId(),
                employee.getDeptId(),
                dept != null ? dept.getDeptType() : null,
                employee.getBranchId(),
                employee.getLevel(),
                employee.getIsContractor()
        );
    }

    private List<String> buildVisibleMenus(DeptType deptType) {
        if (deptType == DeptType.HR || deptType == DeptType.MANAGEMENT) {
            return List.of("projects", "assets", "files", "handover", "audit");
        }
        return List.of("projects", "assets", "files", "audit");
    }

    private List<String> buildCapabilities(DeptType deptType) {
        if (deptType == DeptType.HR || deptType == DeptType.MANAGEMENT) {
            return List.of(
                    "projects.view",
                    "assets.view",
                    "files.view",
                    "files.upload",
                    "files.download",
                    "audit.view",
                    "handover.view"
            );
        }
        return List.of(
                "projects.view",
                "assets.view",
                "files.view",
                "files.upload",
                "files.download",
                "audit.view"
        );
    }

    private void ensureHrOperator(Long operatorEmployeeId) {
        Employees operator = employeesMapper.selectByEmployeeId(operatorEmployeeId);
        if (operator == null) {
            throw new BizException("当前操作人不存在");
        }
        if (operator.getStatus() != EmployeeStatus.ACTIVE) {
            throw new BizException("当前操作人状态不可用");
        }

        Department operatorDept = departmentMapper.selectById(operator.getDeptId());
        if (operatorDept == null) {
            throw new BizException("当前操作人所属部门不存在");
        }
        if (operatorDept.getDeptType() != DeptType.HR) {
            throw new BizException("仅人事部允许执行该操作");
        }
    }
}
