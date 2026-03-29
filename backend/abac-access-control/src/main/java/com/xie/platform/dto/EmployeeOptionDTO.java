package com.xie.platform.dto;

import com.xie.platform.model.enumValue.EmployeeStatus;
import lombok.Data;

@Data
public class EmployeeOptionDTO {

    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private Long deptId;
    private Boolean isContractor;
    private EmployeeStatus status;
}
