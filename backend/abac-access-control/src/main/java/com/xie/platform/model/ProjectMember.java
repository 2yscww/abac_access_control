package com.xie.platform.model;

import com.xie.platform.model.enumValue.ProjectMemberStatus;
import com.xie.platform.model.enumValue.ProjectPhase;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectMember {

    private Long id;
    private Long projectId;
    private Long employeeId;
    private ProjectMemberStatus status;
    private ProjectPhase joinedPhase;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
}
