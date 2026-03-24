package com.xie.platform.dto;

import lombok.Data;

import java.util.List;

@Data
public class EmployeeProfileDTO {
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private Long deptId;
    private String deptType;
    private String deptTypeDesc;
    private Long branchId;
    private String level;
    private Integer levelRank;
    private Boolean isContractor;
    private String status;
    private List<String> visibleMenus;
    private List<String> capabilities;
}
