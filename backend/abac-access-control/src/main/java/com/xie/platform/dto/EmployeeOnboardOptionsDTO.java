package com.xie.platform.dto;

import lombok.Data;

import java.util.List;

@Data
public class EmployeeOnboardOptionsDTO {

    private List<DepartmentOptionDTO> departments;

    private List<BranchOptionDTO> branches;
}
