package com.xie.platform.controller;

import com.xie.platform.common.Response;
import com.xie.platform.dto.AddProjectMemberDTO;
import com.xie.platform.dto.CreateProjectDTO;
import com.xie.platform.dto.ProjectMemberDTO;
import com.xie.platform.dto.ProjectQueryDTO;
import com.xie.platform.dto.UpdateProjectPhaseDTO;
import com.xie.platform.model.Projects;
import com.xie.platform.service.ProjectMemberService;
import com.xie.platform.service.ProjectService;
import com.xie.platform.utils.CurrentUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/project")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectMemberService projectMemberService;

    @PostMapping("/create")
    public Response<Long> createProject(@RequestBody CreateProjectDTO dto) {
        Long employeeId = CurrentUserContext.getRequiredEmployeeId();
        Long projectId = projectService.createProject(dto, employeeId);
        return Response.Success(projectId, "椤圭洰鍒涘缓鎴愬姛");
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
        return Response.Success(null, "椤圭洰闃舵鏇存柊鎴愬姛");
    }

    @GetMapping("/{id}/members")
    public Response<List<ProjectMemberDTO>> listProjectMembers(@PathVariable("id") Long projectId) {
        Long employeeId = CurrentUserContext.getRequiredEmployeeId();
        List<ProjectMemberDTO> result = projectMemberService.listProjectMembers(projectId, employeeId);
        return Response.Success(result, null);
    }

    @PostMapping("/{id}/members")
    public Response<Void> addProjectMember(
            @PathVariable("id") Long projectId,
            @RequestBody AddProjectMemberDTO dto) {
        Long employeeId = CurrentUserContext.getRequiredEmployeeId();
        projectMemberService.addProjectMember(projectId, dto.getEmployeeId(), employeeId);
        return Response.Success(null, "项目成员添加成功");
    }

    @DeleteMapping("/{projectId}/members/{employeeId}")
    public Response<Void> removeProjectMember(
            @PathVariable("projectId") Long projectId,
            @PathVariable("employeeId") Long memberEmployeeId) {
        Long employeeId = CurrentUserContext.getRequiredEmployeeId();
        projectMemberService.removeProjectMember(projectId, memberEmployeeId, employeeId);
        return Response.Success(null, "项目成员移除成功");
    }

    @DeleteMapping("/{id}")
    public Response<Void> deleteProject(@PathVariable("id") Long projectId) {
        Long employeeId = CurrentUserContext.getRequiredEmployeeId();
        projectService.deleteProject(projectId, employeeId);
        return Response.Success(null, "椤圭洰鍒犻櫎鎴愬姛");
    }
}
