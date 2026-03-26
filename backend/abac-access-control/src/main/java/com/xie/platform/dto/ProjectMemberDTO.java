package com.xie.platform.dto;

import com.xie.platform.model.enumValue.DeptType;
import com.xie.platform.model.enumValue.ProjectMemberStatus;
import com.xie.platform.model.enumValue.ProjectPhase;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectMemberDTO {

    private Long id;
    private Long projectId;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private Long deptId;
    private DeptType deptType;
    private ProjectMemberStatus status;
    private ProjectPhase joinedPhase;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
}
