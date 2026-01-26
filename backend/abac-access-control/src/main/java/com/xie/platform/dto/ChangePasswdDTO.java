package com.xie.platform.dto;

import lombok.Data;

@Data
public class ChangePasswdDTO {
    private Long employeeId;

    private String oldPassword;

    private String newPassword;
}
