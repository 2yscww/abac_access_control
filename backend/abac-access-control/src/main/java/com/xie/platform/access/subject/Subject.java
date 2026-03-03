package com.xie.platform.access.subject;

import com.xie.platform.model.enumValue.DeptType;
import com.xie.platform.model.enumValue.EmployeeLevel;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Subject {
    private Long employeeId;
    private Long deptId;
    private DeptType deptType;
    private Long branchId;
    private EmployeeLevel level;
    private Boolean isContractor;
}
