package com.xie.platform.service.impl;

import com.xie.platform.dto.CreateEmployeeDTO;
import com.xie.platform.exception.BizException;
import com.xie.platform.mapper.BranchMapper;
import com.xie.platform.mapper.DepartmentMapper;
import com.xie.platform.mapper.EmployeesMapper;
import com.xie.platform.model.Branch;
import com.xie.platform.model.Department;
import com.xie.platform.model.Employees;
import com.xie.platform.model.enumValue.EmployeeLevel;
import com.xie.platform.model.enumValue.EmployeeStatus;
import com.xie.platform.service.EmployeeAuthService;
import com.xie.platform.service.result.LoginResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xie.platform.utils.JwtUtil;
import com.xie.platform.access.subject.Subject;

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

    @Override
    public LoginResult login(String employeeCode, String rawPassword) {

        LoginResult result = new LoginResult();

        Employees employee = employeesMapper.selectByEmployeeCode(employeeCode);

        // ? 之后需要把返回的信息简化

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

        // 登录成功
        result.setSuccess(true);
        result.setEmployeeId(employee.getEmployeeId());
        result.setMustChangePassword(Boolean.TRUE.equals(employee.getMustChangePassword()));
        result.setMessage("登录成功");



        // 首次登录：下发临时 token，不发正式 token
        if (Boolean.TRUE.equals(employee.getMustChangePassword())) {
            result.setTempToken(jwtUtil.generateTempToken(employee.getEmployeeId()));
            return result;
        }

        // 构造 ABAC Subject
        Subject subject = new Subject(
                employee.getEmployeeId(),
                employee.getDeptId(),
                employee.getBranchId(),
                employee.getLevel(),
                employee.getIsContractor());

        // ? 生成 JWT
        String token = jwtUtil.generateToken(subject);

        //  塞进结果
        result.setToken(token);

        return result;
    }

    @Override
    public String changePassword(String tempToken, String oldPassword, String newPassword) {
        // 验证临时 token，提取 employeeId（非法/过期/scope 不符均抛异常）
        Long employeeId;
        try {
            employeeId = jwtUtil.parseAndValidateTempToken(tempToken);
        } catch (Exception e) {
            throw new BizException("临时凭证无效或已过期，请重新登录");
        }

        Employees employee = employeesMapper.selectByEmployeeId(employeeId);
        if (employee == null) {
            throw new BizException("员工不存在");
        }

        // 校验旧密码
        if (!passwordEncoder.matches(oldPassword, employee.getPassword())) {
            throw new BizException("原密码错误");
        }

        // 更新密码 + 清除强制改密标志
        employeesMapper.updatePassword(employeeId, passwordEncoder.encode(newPassword));

        // 生成并返回正式 token
        Subject subject = new Subject(
                employee.getEmployeeId(),
                employee.getDeptId(),
                employee.getBranchId(),
                employee.getLevel(),
                employee.getIsContractor());
        return jwtUtil.generateToken(subject);
    }

    @Override
    @Transactional
    public void createEmployee(CreateEmployeeDTO dto) {

        // 0. DTO 基础校验
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

        // 2. 校验部门是否存在
        Department dept = departmentMapper.selectById(dto.getDeptId());
        if (dept == null) {
            throw new BizException("部门不存在");
        }

        // 3. 校验分公司是否存在
        Branch branch = branchMapper.selectById(dto.getBranchId());
        if (branch == null) {
            throw new BizException("分公司不存在");
        }

        // 4. 职级合法性校验（int → enum）
        EmployeeLevel level;
        try {
            level = EmployeeLevel.fromRank(dto.getLevel());
        } catch (IllegalArgumentException e) {
            throw new BizException("非法的员工职级");
        }

        // 5. 生成初始密码（系统控制）
        String defaultPwd = "ABACtest";
        String encodedPwd = passwordEncoder.encode(defaultPwd);

        // 6. 构建员工实体
        Employees employee = new Employees();
        employee.setEmployeeCode("PENDING");  // 临时占位，insert 后回填
        employee.setEmployeeName(dto.getEmployeeName());
        employee.setDeptId(dto.getDeptId());
        employee.setBranchId(dto.getBranchId());
        employee.setLevel(level);
        employee.setIsContractor(Boolean.TRUE.equals(dto.getIsContractor()));
        employee.setStatus(EmployeeStatus.ACTIVE);
        employee.setPassword(encodedPwd);
        employee.setMustChangePassword(true);

        // 7. 入库
        employeesMapper.insert(employee);

        // 8. 回填工号（employee_id + 1000 偏移，使工号从 1001 开始）
        String employeeCode = String.valueOf(employee.getEmployeeId() + 1000);
        employeesMapper.updateEmployeeCode(employee.getEmployeeId(), employeeCode);
    }

}
