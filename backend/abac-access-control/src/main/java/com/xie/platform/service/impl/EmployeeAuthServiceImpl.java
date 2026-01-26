package com.xie.platform.service.impl;

import com.xie.platform.mapper.EmployeesMapper;
import com.xie.platform.model.Employees;
import com.xie.platform.model.enumValue.EmployeeStatus;
import com.xie.platform.service.EmployeeAuthService;
import com.xie.platform.service.result.LoginResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class EmployeeAuthServiceImpl implements EmployeeAuthService {

    @Autowired
    private EmployeesMapper employeesMapper;

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

        return result;
    }

    @Override
    public void changePassword(Long employeeId, String oldPassword, String newPassword) {
        // TODO 完善强制修改密码的功能
        Employees employee = employeesMapper.selectByEmployeeId(employeeId);

    if (employee == null) {
        throw new RuntimeException("员工不存在");
    }

    // 校验旧密码
    if (!passwordEncoder.matches(oldPassword, employee.getPassword())) {
        throw new RuntimeException("原密码错误");
    }

    // 加密新密码
    String encodedNewPassword = passwordEncoder.encode(newPassword);

    // 更新密码 + 关闭强制改密标志
    employeesMapper.updatePassword(
        employeeId,
        encodedNewPassword
    );
        
    }
}
