package com.xie.platform.dto;

import lombok.Data;

@Data
public class ChangePasswdDTO {
    private String oldPassword;

    private String newPassword;
}
