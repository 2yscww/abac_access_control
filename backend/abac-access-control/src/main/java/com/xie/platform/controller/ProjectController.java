package com.xie.platform.controller;

import com.xie.platform.common.Response;
import com.xie.platform.dto.CreateProjectDTO;
import com.xie.platform.dto.ProjectQueryDTO;
import com.xie.platform.dto.UpdateProjectPhaseDTO;
import com.xie.platform.model.Projects;
import com.xie.platform.service.ProjectService;
import com.xie.platform.utils.CurrentUserContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/project")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @PostMapping("/create")
    public Response<Long> createProject(@RequestBody CreateProjectDTO dto) {
        Long employeeId = CurrentUserContext.getRequiredEmployeeId();
        Long projectId = projectService.createProject(dto, employeeId);
        return Response.Success(projectId, "项目创建成功");
    }

    @GetMapping("/{id}")
    public Response<Projects> getProject(@PathVariable("id") Long projectId) {
        Long employeeId = CurrentUserContext.getRequiredEmployeeId();
        Projects project = projectService.getProjectById(projectId, employeeId);
        return Response.Success(project, null);
    }

    @GetMapping("/list")
    public Response<Map<String, Object>> queryProjects(ProjectQueryDTO query) {
        Long employeeId = CurrentUserContext.getRequiredEmployeeId();
        Map<String, Object> result = projectService.queryProjects(query, employeeId);
        return Response.Success(result, null);
    }

    @PutMapping("/phase")
    public Response<Void> updateProjectPhase(@RequestBody UpdateProjectPhaseDTO dto) {
        Long employeeId = CurrentUserContext.getRequiredEmployeeId();
        projectService.updateProjectPhase(dto, employeeId);
        return Response.Success(null, "项目阶段更新成功");
    }

    @DeleteMapping("/{id}")
    public Response<Void> deleteProject(@PathVariable("id") Long projectId) {
        Long employeeId = CurrentUserContext.getRequiredEmployeeId();
        projectService.deleteProject(projectId, employeeId);
        return Response.Success(null, "项目删除成功");
    }
}
