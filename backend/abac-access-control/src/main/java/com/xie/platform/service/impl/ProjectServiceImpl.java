package com.xie.platform.service.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.pep.PolicyEnforcementPoint;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.access.resource.ResourceType;
import com.xie.platform.dto.CreateProjectDTO;
import com.xie.platform.dto.ProjectQueryDTO;
import com.xie.platform.dto.UpdateProjectPhaseDTO;
import com.xie.platform.exception.BizException;
import com.xie.platform.mapper.DepartmentMapper;
import com.xie.platform.mapper.EmployeesMapper;
import com.xie.platform.mapper.ProjectMapper;
import com.xie.platform.model.Department;
import com.xie.platform.model.Employees;
import com.xie.platform.model.Projects;
import com.xie.platform.model.enumValue.DeptType;
import com.xie.platform.model.enumValue.EmployeeStatus;
import com.xie.platform.model.enumValue.ProjectPhase;
import com.xie.platform.model.enumValue.SecurityLevel;
import com.xie.platform.service.ProjectMemberService;
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
    private EmployeesMapper employeesMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private PolicyEnforcementPoint pep;

    @Autowired
    private ProjectMemberService projectMemberService;

    @Override
    @Transactional
    public Long createProject(CreateProjectDTO dto, Long creatorEmployeeId) {
        if (dto.getProjectName() == null || dto.getProjectName().isBlank()) {
            throw new BizException("项目名称不能为空");
        }
        if (dto.getSecurityLevel() == null) {
            throw new BizException("项目密级不能为空");
        }
        if (dto.getOwnerId() == null) {
            throw new BizException("当前阶段负责人不能为空");
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

        validateStageOwner(projectPhase, dto.getOwnerId());

        pep.checkAccess(
                creatorEmployeeId,
                Resource.builder()
                        .type(ResourceType.PROJECT)
                        .projectId(null)
                        .projectPhase(projectPhase)
                        .securityLevel(securityLevel)
                        .creatorId(creatorEmployeeId)
                        .ownerId(dto.getOwnerId())
                        .build(),
                Action.WRITE
        );

        Projects project = new Projects();
        project.setProjectName(dto.getProjectName());
        project.setProjectPhase(projectPhase);
        project.setSecurityLevel(securityLevel);
        project.setCreatedByEmployeeId(creatorEmployeeId);
        project.setOwnerId(dto.getOwnerId());

        projectMapper.insert(project);
        projectMemberService.initializeProjectMembers(
                project.getProjectId(),
                creatorEmployeeId,
                dto.getOwnerId(),
                projectPhase
        );
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
        if (dto.getNextOwnerId() == null) {
            throw new BizException("目标阶段负责人不能为空");
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

        validateStageOwner(newPhase, dto.getNextOwnerId());
        projectMapper.updatePhase(dto.getProjectId(), newPhase.getCode(), dto.getNextOwnerId());
        projectMemberService.syncMembersForPhaseTransition(dto.getProjectId(), newPhase, dto.getNextOwnerId());
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

        projectMemberService.deleteByProjectId(projectId);
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
                .resourceId(project.getProjectId())
                .projectId(project.getProjectId())
                .projectPhase(project.getProjectPhase())
                .securityLevel(project.getSecurityLevel())
                .creatorId(project.getCreatedByEmployeeId())
                .ownerId(project.getOwnerId())
                .build();
    }

    private void validateStageOwner(ProjectPhase projectPhase, Long ownerId) {
        Employees owner = employeesMapper.selectByEmployeeId(ownerId);
        if (owner == null) {
            throw new BizException("阶段负责人不存在");
        }
        if (owner.getStatus() != EmployeeStatus.ACTIVE) {
            throw new BizException("阶段负责人状态不可用");
        }

        Department department = departmentMapper.selectById(owner.getDeptId());
        if (department == null) {
            throw new BizException("阶段负责人所属部门不存在");
        }

        DeptType expectedDeptType = getStageOwnerDept(projectPhase);
        if (department.getDeptType() != expectedDeptType) {
            throw new BizException("目标负责人不属于阶段主责部门：" + expectedDeptType.getDesc());
        }
        if (department.getManagerId() == null) {
            throw new BizException("阶段主责部门尚未配置 manager_id：" + expectedDeptType.getDesc());
        }
        if (!department.getManagerId().equals(ownerId)) {
            throw new BizException("目标负责人必须等于阶段主责部门的 manager_id");
        }
    }

    private DeptType getStageOwnerDept(ProjectPhase projectPhase) {
        return switch (projectPhase) {
            case INIT -> DeptType.MANAGEMENT;
            case REQUIREMENT -> DeptType.PRODUCT;
            case DEVELOPMENT -> DeptType.RD;
            case TEST -> DeptType.QA;
            case RELEASE -> DeptType.OPS;
            case ARCHIVED -> DeptType.MANAGEMENT;
        };
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
