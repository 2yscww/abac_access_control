package com.xie.platform.model;

import java.time.LocalDateTime;

import com.xie.platform.model.enumValue.EmployeeLevel;
import com.xie.platform.model.enumValue.EmployeeStatus;

import lombok.Data;

@Data
public class Employees {
    // TODO 做出员工的登录功能
    private Long employeeId;
    private String employeeName;

    private Long deptId;
    private Long branchId;

    // ? 安全级别
    private EmployeeLevel level;

    // ? JSON 字符串 当前员工负责的项目
    private String currentProjects; 
    

    private Boolean isContractor;

    private EmployeeStatus status;

    private String password;

    private Boolean mustChangePassword;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
