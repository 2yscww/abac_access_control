package com.xie.platform.dto;

import lombok.Data;

@Data
public class PhaseOwnerPreviewDTO {

    private Integer targetPhase;
    private String targetPhaseDesc;

    private Long deptId;
    private String deptName;
    private String deptType;

    private Long employeeId;
    private String employeeCode;
    private String employeeName;

    private Boolean configured;
    private String message;
}
