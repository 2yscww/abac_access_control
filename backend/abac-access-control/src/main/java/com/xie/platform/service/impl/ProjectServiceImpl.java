package com.xie.platform.service.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.pep.PolicyEnforcementPoint;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.access.resource.ResourceType;
import com.xie.platform.dto.CreateProjectDTO;
import com.xie.platform.dto.ProjectQueryDTO;
import com.xie.platform.dto.UpdateProjectPhaseDTO;
import com.xie.platform.exception.BizException;
import com.xie.platform.mapper.ProjectMapper;
import com.xie.platform.model.Projects;
import com.xie.platform.model.enumValue.ProjectPhase;
import com.xie.platform.model.enumValue.SecurityLevel;
import com.xie.platform.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProjectServiceImpl implements ProjectService {

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private PolicyEnforcementPoint pep;

    @Override
    @Transactional
    public Long createProject(CreateProjectDTO dto, Long creatorEmployeeId) {
        if (dto.getProjectName() == null || dto.getProjectName().isBlank()) {
            throw new BizException("项目名称不能为空");
        }
        if (dto.getSecurityLevel() == null) {
            throw new BizException("项目密级不能为空");
        }

        Projects existProject = projectMapper.selectByName(dto.getProjectName());
        if (existProject != null) {
            throw new BizException("项目名称已存在");
        }

        SecurityLevel securityLevel;
        try {
            securityLevel = SecurityLevel.fromLevel(dto.getSecurityLevel());
        } catch (IllegalArgumentException exception) {
            throw new BizException("非法的项目密级");
        }

        ProjectPhase projectPhase;
        if (dto.getProjectPhase() == null) {
            projectPhase = ProjectPhase.INIT;
        } else {
            try {
                projectPhase = ProjectPhase.fromCode(dto.getProjectPhase());
            } catch (IllegalArgumentException exception) {
                throw new BizException("非法的项目阶段");
            }
        }

        pep.checkAccess(
                creatorEmployeeId,
                Resource.builder()
                        .type(ResourceType.PROJECT)
                        .projectPhase(projectPhase)
                        .securityLevel(securityLevel)
                        .creatorId(creatorEmployeeId)
                        .build(),
                Action.WRITE
        );

        Projects project = new Projects();
        project.setProjectName(dto.getProjectName());
        project.setProjectPhase(projectPhase);
        project.setSecurityLevel(securityLevel);
        project.setCreatedByEmployeeId(creatorEmployeeId);

        projectMapper.insert(project);
        return project.getProjectId();
    }

    @Override
    public Projects getProjectById(Long projectId, Long employeeId) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }

        pep.checkProjectAccess(employeeId, projectId, Action.READ);

        Projects project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BizException("项目不存在");
        }
        return project;
    }

    @Override
    public Map<String, Object> queryProjects(ProjectQueryDTO query, Long employeeId) {
        int pageNum = normalizePageNum(query.getPageNum());
        int pageSize = normalizePageSize(query.getPageSize());

        List<Projects> matchedProjects = projectMapper.selectByCondition(copyQueryWithoutPagination(query));
        List<Projects> accessibleProjects = matchedProjects.stream()
                .filter(project -> pep.decideAccess(employeeId, buildProjectResource(project), Action.READ).isAllowed())
                .toList();

        return buildPageResult(accessibleProjects, pageNum, pageSize);
    }

    @Override
    @Transactional
    public void updateProjectPhase(UpdateProjectPhaseDTO dto, Long employeeId) {
        if (dto.getProjectId() == null) {
            throw new BizException("项目ID不能为空");
        }
        if (dto.getNewPhase() == null) {
            throw new BizException("新阶段不能为空");
        }

        pep.checkProjectAccess(employeeId, dto.getProjectId(), Action.ADVANCE_PHASE);

        Projects project = projectMapper.selectById(dto.getProjectId());
        if (project == null) {
            throw new BizException("项目不存在");
        }

        ProjectPhase newPhase;
        try {
            newPhase = ProjectPhase.fromCode(dto.getNewPhase());
        } catch (IllegalArgumentException exception) {
            throw new BizException("非法的项目阶段");
        }

        if (project.getProjectPhase() == ProjectPhase.ARCHIVED) {
            throw new BizException("已归档项目不能修改阶段");
        }

        projectMapper.updatePhase(dto.getProjectId(), newPhase.getCode());
    }

    @Override
    @Transactional
    public void deleteProject(Long projectId, Long employeeId) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }

        pep.checkProjectAccess(employeeId, projectId, Action.DELETE);

        Projects project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BizException("项目不存在");
        }

        projectMapper.deleteById(projectId);
    }

    private ProjectQueryDTO copyQueryWithoutPagination(ProjectQueryDTO query) {
        ProjectQueryDTO copiedQuery = new ProjectQueryDTO();
        copiedQuery.setProjectName(query.getProjectName());
        copiedQuery.setProjectPhase(query.getProjectPhase());
        copiedQuery.setSecurityLevel(query.getSecurityLevel());
        copiedQuery.setCreatedByEmployeeId(query.getCreatedByEmployeeId());
        copiedQuery.setPageNum(null);
        copiedQuery.setPageSize(null);
        return copiedQuery;
    }

    private Resource buildProjectResource(Projects project) {
        return Resource.builder()
                .type(ResourceType.PROJECT)
                .projectPhase(project.getProjectPhase())
                .securityLevel(project.getSecurityLevel())
                .creatorId(project.getCreatedByEmployeeId())
                .build();
    }

    private Map<String, Object> buildPageResult(List<?> data, int pageNum, int pageSize) {
        int total = data.size();
        int fromIndex = Math.min((pageNum - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);

        Map<String, Object> result = new HashMap<>();
        result.put("list", data.subList(fromIndex, toIndex));
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }
}
