package com.xie.platform.model;

import com.xie.platform.model.enumValue.DeptType;
import lombok.Data;

@Data
public class Department {
    private Long deptId;
    private String deptName;
    private DeptType deptType;
}
