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

    /***
     * jwt信息
     */
    private String token;

    /**
     * 临时 token（仅 mustChangePassword=true 时有值，用于认证改密请求）
     */
    private String tempToken;
}
