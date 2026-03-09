package com.xie.platform.service;

import com.xie.platform.dto.CreateProjectDTO;
import com.xie.platform.dto.ProjectQueryDTO;
import com.xie.platform.dto.UpdateProjectPhaseDTO;
import com.xie.platform.model.Projects;

import java.util.List;
import java.util.Map;

public interface ProjectService {

    /**
     * 创建项目
     *
     * @param dto 项目创建信息
     * @param creatorEmployeeId 创建人ID（从JWT中获取）
     * @return 创建的项目ID
     */
    Long createProject(CreateProjectDTO dto, Long creatorEmployeeId);

    /**
     * 根据ID查询项目详情
     *
     * @param projectId 项目ID
     * @param employeeId 请求员工ID（用于权限检查）
     * @return 项目详情
     */
    Projects getProjectById(Long projectId, Long employeeId);

    /**
     * 条件查询项目列表（分页）
     *
     * @param query 查询条件
     * @return 项目列表 + 分页信息
     */
    Map<String, Object> queryProjects(ProjectQueryDTO query);

    /**
     * 更新项目阶段
     *
     * @param dto 更新信息
     * @param employeeId 请求员工ID（用于权限检查）
     */
    void updateProjectPhase(UpdateProjectPhaseDTO dto, Long employeeId);

    /**
     * 删除项目
     *
     * @param projectId 项目ID
     * @param employeeId 请求员工ID（用于权限检查）
     */
    void deleteProject(Long projectId, Long employeeId);
}
