package com.xie.platform.service;

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

    LoginResult login(String employeeName, String rawPassword);

    /**
    *    强制修改密码
    */
    void changePassword(Long employeeId, String oldPassword, String newPassword);


}
