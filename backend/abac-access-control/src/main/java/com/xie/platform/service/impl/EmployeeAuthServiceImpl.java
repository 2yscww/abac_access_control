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
}
