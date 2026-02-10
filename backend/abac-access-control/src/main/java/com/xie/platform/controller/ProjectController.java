package com.xie.platform.controller;

import com.xie.platform.common.Response;
import com.xie.platform.dto.CreateProjectDTO;
import com.xie.platform.dto.ProjectQueryDTO;
import com.xie.platform.dto.UpdateProjectPhaseDTO;
import com.xie.platform.model.Projects;
import com.xie.platform.service.ProjectService;
import com.xie.platform.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 项目管理 Controller
 */
@RestController
@RequestMapping("/api/project")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 创建项目
     * POST /api/project/create
     */
    @PostMapping("/create")
    public Response<Long> createProject(
            @RequestBody CreateProjectDTO dto,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 从 JWT 中解析 employeeId
        Long employeeId = extractEmployeeIdFromToken(authHeader);
        if (employeeId == null) {
            return Response.Fail(null, "未登录或token无效");
        }

        Long projectId = projectService.createProject(dto, employeeId);
        return Response.Success(projectId, "项目创建成功");
    }

    /**
     * 查询项目详情
     * GET /api/project/{id}
     */
    @GetMapping("/{id}")
    public Response<Projects> getProject(@PathVariable("id") Long projectId) {
        Projects project = projectService.getProjectById(projectId);
        return Response.Success(project, null);
    }

    /**
     * 条件查询项目列表（分页）
     * GET /api/project/list
     */
    @GetMapping("/list")
    public Response<Map<String, Object>> queryProjects(ProjectQueryDTO query) {
        Map<String, Object> result = projectService.queryProjects(query);
        return Response.Success(result, null);
    }

    /**
     * 更新项目阶段
     * PUT /api/project/phase
     */
    @PutMapping("/phase")
    public Response<Void> updateProjectPhase(@RequestBody UpdateProjectPhaseDTO dto) {
        projectService.updateProjectPhase(dto);
        return Response.Success(null, "项目阶段更新成功");
    }

    /**
     * 删除项目
     * DELETE /api/project/{id}
     */
    @DeleteMapping("/{id}")
    public Response<Void> deleteProject(@PathVariable("id") Long projectId) {
        projectService.deleteProject(projectId);
        return Response.Success(null, "项目删除成功");
    }

    /**
     * 从 Authorization 请求头中提取 employeeId
     *
     * @param authHeader Authorization 请求头（格式：Bearer <token>）
     * @return employeeId，解析失败返回 null
     */
    private Long extractEmployeeIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        try {
            String token = authHeader.substring(7); // 去掉 "Bearer " 前缀
            Claims claims = jwtUtil.parseToken(token);
            String subject = claims.getSubject(); // employeeId 存储在 subject 中
            return Long.parseLong(subject);
        } catch (Exception e) {
            // token 解析失败（过期、非法等）
            return null;
        }
    }
}
