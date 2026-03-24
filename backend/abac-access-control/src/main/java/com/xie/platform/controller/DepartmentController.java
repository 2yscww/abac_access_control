package com.xie.platform.controller;

import com.xie.platform.common.Response;
import com.xie.platform.dto.AssignDepartmentManagerDTO;
import com.xie.platform.dto.DepartmentManagerHandoverTodoDTO;
import com.xie.platform.dto.EmployeeOptionDTO;
import com.xie.platform.service.DepartmentService;
import com.xie.platform.utils.CurrentUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/department")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @PutMapping("/manager")
    public Response<Void> assignDepartmentManager(@RequestBody AssignDepartmentManagerDTO dto) {
        Long employeeId = CurrentUserContext.getRequiredEmployeeId();
        departmentService.assignDepartmentManager(dto, employeeId);
        return Response.Success(null, "部门负责人更新成功");
    }

    @GetMapping("/manager-handover-todos")
    public Response<List<DepartmentManagerHandoverTodoDTO>> queryManagerHandoverTodos() {
        Long employeeId = CurrentUserContext.getRequiredEmployeeId();
        List<DepartmentManagerHandoverTodoDTO> result = departmentService.queryManagerHandoverTodos(employeeId);
        return Response.Success(result, null);
    }

    @GetMapping("/{deptId}/manager-candidates")
    public Response<List<EmployeeOptionDTO>> queryManagerCandidates(@PathVariable("deptId") Long deptId) {
        Long employeeId = CurrentUserContext.getRequiredEmployeeId();
        List<EmployeeOptionDTO> result = departmentService.queryManagerCandidates(deptId, employeeId);
        return Response.Success(result, null);
    }
}
