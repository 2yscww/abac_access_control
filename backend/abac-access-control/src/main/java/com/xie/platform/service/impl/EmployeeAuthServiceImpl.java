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

    @Override
    public LoginResult login(String employeeName, String rawPassword) {

        LoginResult result = new LoginResult();

        Employees employee = employeesMapper.selectByEmployeeName(employeeName);

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
        // TODO 接入JWT功能，为subject打好基础
        return result;
    }

    @Override
    public void changePassword(Long employeeId, String oldPassword, String newPassword) {
        Employees employee = employeesMapper.selectByEmployeeId(employeeId);

        if (employee == null) {
            throw new BizException("员工不存在");
        }

        // 校验旧密码
        if (!passwordEncoder.matches(oldPassword, employee.getPassword())) {
            throw new BizException("原密码错误");
        }

        // 加密新密码
        String encodedNewPassword = passwordEncoder.encode(newPassword);

        // 更新密码 + 关闭强制改密标志
        employeesMapper.updatePassword(employeeId, encodedNewPassword);
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

        // 1. 员工唯一性校验（employee_name 作为登录名）
        Employees exist = employeesMapper.selectByEmployeeName(dto.getEmployeeName());
        if (exist != null) {
            throw new BizException("员工已存在");
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
    }

}
