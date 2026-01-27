package com.xie.platform.access.subject;

import com.xie.platform.model.enumValue.EmployeeLevel;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Subject {
    // ? 先定义员工的 subject
    private Long employeeId;
    private Long deptId;
    private Long branchId;
    private EmployeeLevel level;
    private Boolean isContractor;
}
