package com.xie.platform.service;

import com.xie.platform.dto.CreateProjectDTO;
import com.xie.platform.dto.PhaseOwnerPreviewDTO;
import com.xie.platform.dto.ProjectQueryDTO;
import com.xie.platform.dto.UpdateProjectPhaseDTO;
import com.xie.platform.model.Projects;

import java.util.Map;

public interface ProjectService {

    Long createProject(CreateProjectDTO dto, Long creatorEmployeeId);

    Projects getProjectById(Long projectId, Long employeeId);

    Map<String, Object> queryProjects(ProjectQueryDTO query, Long employeeId);

    PhaseOwnerPreviewDTO getPhaseOwnerPreview(Long projectId, Integer targetPhase, Long employeeId);

    void updateProjectPhase(UpdateProjectPhaseDTO dto, Long employeeId);

    void deleteProject(Long projectId, Long employeeId);
}
