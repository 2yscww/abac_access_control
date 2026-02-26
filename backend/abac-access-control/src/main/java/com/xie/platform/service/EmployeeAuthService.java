package com.xie.platform.service;

import com.xie.platform.dto.CreateEmployeeDTO;
import com.xie.platform.service.result.LoginResult;

public interface EmployeeAuthService {

    /**
     * 员工登录认证
     *
     * @param employeeName 员工登录名
     * @param rawPassword  明文密码
     * @return 登录结果
     */

    // TODO 继续完善登录功能

    LoginResult login(String employeeCode, String rawPassword);



    /**
     * 强制修改密码，返回正式 token
     *
     * @param tempToken   首次登录时下发的临时凭证
     * @param oldPassword 旧密码（初始密码）
     * @param newPassword 新密码
     * @return 正式 JWT token
     */
    String changePassword(String tempToken, String oldPassword, String newPassword);

    /**
     * 创建员工
     */
    void createEmployee(CreateEmployeeDTO dto);

}
