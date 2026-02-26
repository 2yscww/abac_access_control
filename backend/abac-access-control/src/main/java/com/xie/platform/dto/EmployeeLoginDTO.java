package com.xie.platform.dto;

import lombok.Data;

@Data
public class EmployeeLoginDTO {
    private String employeeCode;  // 工号（如 1001）
    private String password;
}
