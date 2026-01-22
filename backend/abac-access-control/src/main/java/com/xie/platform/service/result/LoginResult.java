package com.xie.platform.service.result;

import lombok.Data;

@Data
public class LoginResult {
     /**
     * 是否登录成功
     */
    private boolean success;

    /**
     * 员工 ID（仅在成功时有值）
     */
    private Long employeeId;

    /**
     * 是否必须修改密码
     */
    private boolean mustChangePassword;

    /**
     * 失败或提示信息
     */
    private String message;
}
