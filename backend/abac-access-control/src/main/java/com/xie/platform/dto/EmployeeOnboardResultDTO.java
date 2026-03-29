package com.xie.platform.dto;

import lombok.Data;

@Data
public class EmployeeOnboardResultDTO {

    private Long employeeId;

    private String employeeCode;

    private String employeeName;

    private Long deptId;

    private String deptName;

    private Long branchId;

    private String branchName;

    private String level;

    private Integer levelRank;

    private Boolean isContractor;

    private String initialPassword;

    private Boolean mustChangePassword;
}
