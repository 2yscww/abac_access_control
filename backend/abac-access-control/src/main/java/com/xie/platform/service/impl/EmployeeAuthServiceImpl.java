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
import com.xie.platform.service.PolicyConfigService;
import com.xie.platform.service.ProjectMemberService;
import com.xie.platform.service.result.LoginResult;
import com.xie.platform.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmployeeAuthServiceImpl implements EmployeeAuthService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeAuthServiceImpl.class);

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

    @Autowired
    private ProjectMemberService projectMemberService;

    @Autowired
    private PolicyConfigService policyConfigService;

    @Override
    public LoginResult login(String employeeCode, String rawPassword) {
        LoginResult result = new LoginResult();
        Employees employee = employeesMapper.selectByEmployeeCode(employeeCode);

        if (employee == null) {
            result.setSuccess(false);
            result.setMessage("员工不存在");
            safeRecordSecurityEvent(
                    null,
                    "AUTH",
                    null,
                    Action.LOGIN,
                    "员工不存在",
                    buildAuthFailureDetail(employeeCode, null, "员工不存在")
            );
            return result;
        }
        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            result.setSuccess(false);
            result.setMessage("员工状态不可用");
            safeRecordSecurityEvent(
                    employee.getEmployeeId(),
                    "AUTH",
                    employee.getEmployeeId(),
                    Action.LOGIN,
                    "员工状态不可用",
                    buildAuthFailureDetail(employeeCode, employee, "员工状态不可用")
            );
            return result;
        }
        if (!passwordEncoder.matches(rawPassword, employee.getPassword())) {
            result.setSuccess(false);
            result.setMessage("密码错误");
            safeRecordSecurityEvent(
                    employee.getEmployeeId(),
                    "AUTH",
                    employee.getEmployeeId(),
                    Action.LOGIN,
                    "密码错误",
                    buildAuthFailureDetail(employeeCode, employee, "密码错误")
            );
            return result;
        }

        result.setSuccess(true);
        result.setEmployeeId(employee.getEmployeeId());
        result.setMustChangePassword(Boolean.TRUE.equals(employee.getMustChangePassword()));
        result.setMessage("登录成功");

        if (Boolean.TRUE.equals(employee.getMustChangePassword())) {
            result.setTempToken(jwtUtil.generateTempToken(employee.getEmployeeId()));
            auditLogService.recordBusinessEvent(
                    employee.getEmployeeId(),
                    "AUTH",
                    employee.getEmployeeId(),
                    Action.LOGIN,
                    buildLoginSuccessDetail(employee, "TEMP_TOKEN", true)
            );
            return result;
        }

        result.setToken(jwtUtil.generateToken(buildSubject(employee)));
        auditLogService.recordBusinessEvent(
                employee.getEmployeeId(),
                "AUTH",
                employee.getEmployeeId(),
                Action.LOGIN,
                buildLoginSuccessDetail(employee, "ACCESS_TOKEN", false)
        );
        return result;
    }

    @Override
    public String changePassword(String tempToken, String oldPassword, String newPassword) {
        Long employeeId;
        try {
            employeeId = jwtUtil.parseAndValidateTempToken(tempToken);
        } catch (Exception exception) {
            safeRecordSecurityEvent(
                    null,
                    "AUTH",
                    null,
                    Action.CHANGE_PASSWORD,
                    "临时凭证无效或已过期，请重新登录",
                    Map.of(
                            "credentialType", "TEMP_TOKEN",
                            "result", "FAILURE"
                    )
            );
            throw new BizException("临时凭证无效或已过期，请重新登录");
        }

        Employees employee = employeesMapper.selectByEmployeeId(employeeId);
        if (employee == null) {
            safeRecordSecurityEvent(
                    employeeId,
                    "AUTH",
                    employeeId,
                    Action.CHANGE_PASSWORD,
                    "员工不存在",
                    Map.of(
                            "employeeId", employeeId,
                            "credentialType", "TEMP_TOKEN",
                            "result", "FAILURE"
                    )
            );
            throw new BizException("员工不存在");
        }
        if (!passwordEncoder.matches(oldPassword, employee.getPassword())) {
            safeRecordSecurityEvent(
                    employeeId,
                    "AUTH",
                    employeeId,
                    Action.CHANGE_PASSWORD,
                    "原密码错误",
                    Map.of(
                            "employeeId", employeeId,
                            "employeeCode", employee.getEmployeeCode(),
                            "employeeName", employee.getEmployeeName(),
                            "credentialType", "TEMP_TOKEN",
                            "result", "FAILURE"
                    )
            );
            throw new BizException("原密码错误");
        }

        employeesMapper.updatePassword(employeeId, passwordEncoder.encode(newPassword));
        String token = jwtUtil.generateToken(buildSubject(employee));
        auditLogService.recordBusinessEvent(
                employeeId,
                "AUTH",
                employeeId,
                Action.CHANGE_PASSWORD,
                Map.of(
                        "employeeId", employeeId,
                        "employeeCode", employee.getEmployeeCode(),
                        "employeeName", employee.getEmployeeName(),
                        "credentialType", "TEMP_TOKEN",
                        "issuedTokenType", "ACCESS_TOKEN",
                        "result", "SUCCESS"
                )
        );
        return token;
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
        projectMemberService.deactivateByEmployeeId(targetEmployee.getEmployeeId());

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
        boolean policyAdmin = policyConfigService.isPolicyAdmin(employeeId);
        profile.setVisibleMenus(buildVisibleMenus(dept.getDeptType(), policyAdmin));
        profile.setCapabilities(buildCapabilities(dept.getDeptType(), policyAdmin));
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

    private List<String> buildVisibleMenus(DeptType deptType, boolean policyAdmin) {
        List<String> menus = new ArrayList<>(List.of("projects", "assets", "files"));
        if (deptType == DeptType.HR || deptType == DeptType.MANAGEMENT) {
            menus.add("handover");
        }
        if (deptType == DeptType.MANAGEMENT) {
            menus.add("audit");
        }
        if (policyAdmin) {
            menus.add("policy");
        }
        return List.copyOf(menus);
    }

    private List<String> buildCapabilities(DeptType deptType, boolean policyAdmin) {
        List<String> capabilities = new ArrayList<>(List.of(
                "projects.view",
                "assets.view",
                "files.view",
                "files.upload",
                "files.download"
        ));

        if (deptType == DeptType.HR) {
            capabilities.add("handover.view");
            capabilities.add("handover.offboard");
        }
        if (deptType == DeptType.MANAGEMENT) {
            capabilities.add("handover.view");
            capabilities.add("handover.assign");
            capabilities.add("audit.view");
        }
        if (policyAdmin) {
            capabilities.add("policy.manage");
        }
        return List.copyOf(capabilities);
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

    private Map<String, Object> buildLoginSuccessDetail(Employees employee, String tokenType, boolean mustChangePassword) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("employeeId", employee.getEmployeeId());
        detail.put("employeeCode", employee.getEmployeeCode());
        detail.put("employeeName", employee.getEmployeeName());
        detail.put("mustChangePassword", mustChangePassword);
        detail.put("issuedTokenType", tokenType);
        detail.put("result", "SUCCESS");
        return detail;
    }

    private Map<String, Object> buildAuthFailureDetail(String employeeCode, Employees employee, String reason) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("employeeCode", employeeCode);
        detail.put("employeeId", employee != null ? employee.getEmployeeId() : null);
        detail.put("employeeName", employee != null ? employee.getEmployeeName() : null);
        detail.put("result", "FAILURE");
        detail.put("reason", reason);
        return detail;
    }

    private void safeRecordSecurityEvent(
            Long employeeId,
            String resourceType,
            Long resourceId,
            Action action,
            String denyReason,
            Map<String, Object> detail) {
        try {
            auditLogService.recordSecurityEvent(employeeId, resourceType, resourceId, action, denyReason, detail);
        } catch (RuntimeException exception) {
            log.warn("Failed to record security audit event for action {}", action, exception);
        }
    }
}
