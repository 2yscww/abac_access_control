package com.xie.platform.dto;

import com.xie.platform.model.Projects;
import com.xie.platform.model.enumValue.DeptType;
import lombok.Data;

import java.util.List;

@Data
public class DepartmentManagerHandoverTodoDTO {

    private Long deptId;
    private String deptName;
    private DeptType deptType;

    private Long managerId;
    private String managerCode;
    private String managerName;

    private Integer affectedProjectCount;
    private List<Projects> affectedProjects;
}
