package com.xie.platform.dto;

import lombok.Data;

@Data
public class CreateEmployeeDTO {
    
    private String employeeName;

    private Long deptId;
    private Long branchId;

    private Integer level;

    private Boolean isContractor;
}
