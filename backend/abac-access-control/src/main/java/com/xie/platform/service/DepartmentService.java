package com.xie.platform.service;

import com.xie.platform.dto.AssignDepartmentManagerDTO;
import com.xie.platform.dto.DepartmentManagerHandoverTodoDTO;
import com.xie.platform.dto.EmployeeOptionDTO;

import java.util.List;

public interface DepartmentService {

    void assignDepartmentManager(AssignDepartmentManagerDTO dto, Long operatorEmployeeId);

    List<DepartmentManagerHandoverTodoDTO> queryManagerHandoverTodos(Long operatorEmployeeId);

    List<EmployeeOptionDTO> queryManagerCandidates(Long deptId, Long operatorEmployeeId);
}
