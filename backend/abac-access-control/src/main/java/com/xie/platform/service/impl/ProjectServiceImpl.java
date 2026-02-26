package com.xie.platform.service.impl;

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

    @Override
    @Transactional
    public Long createProject(CreateProjectDTO dto, Long creatorEmployeeId) {

        // 1. 参数校验
        if (dto.getProjectName() == null || dto.getProjectName().isBlank()) {
            throw new BizException("项目名称不能为空");
        }
        if (dto.getSecurityLevel() == null) {
            throw new BizException("项目保密等级不能为空");
        }

        // 2. 项目名称唯一性校验
        Projects existProject = projectMapper.selectByName(dto.getProjectName());
        if (existProject != null) {
            throw new BizException("项目名称已存在");
        }

        // 3. 校验保密等级合法性
        SecurityLevel securityLevel;
        try {
            securityLevel = SecurityLevel.fromLevel(dto.getSecurityLevel());
        } catch (IllegalArgumentException e) {
            throw new BizException("非法的保密等级");
        }

        // 4. 校验项目阶段合法性（默认为立项阶段）
        ProjectPhase projectPhase;
        if (dto.getProjectPhase() == null) {
            projectPhase = ProjectPhase.INIT; // 默认立项阶段
        } else {
            try {
                projectPhase = ProjectPhase.fromCode(dto.getProjectPhase());
            } catch (IllegalArgumentException e) {
                throw new BizException("非法的项目阶段");
            }
        }



        // 6. 构建项目实体
        Projects project = new Projects();
        // ? project.setProjectId(projectId); 业务代码不参与生成项目id
        project.setProjectName(dto.getProjectName());
        project.setProjectPhase(projectPhase);
        project.setSecurityLevel(securityLevel);
        project.setCreatedByEmployeeId(creatorEmployeeId);

        // 7. 插入数据库
        projectMapper.insert(project);

        return project.getProjectId();
    }

    @Override
    public Projects getProjectById(Long projectId) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }

        Projects project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BizException("项目不存在");
        }

        return project;
    }

    @Override
    public Map<String, Object> queryProjects(ProjectQueryDTO query) {

        // 1. 参数校验与默认值
        if (query.getPageNum() == null || query.getPageNum() < 1) {
            query.setPageNum(1);
        }
        if (query.getPageSize() == null || query.getPageSize() < 1) {
            query.setPageSize(10);
        }

        // 2. 计算分页偏移量（MyBatis LIMIT offset, size）
        int offset = (query.getPageNum() - 1) * query.getPageSize();
        query.setPageNum(offset); // 复用 pageNum 字段传递 offset

        // 3. 查询数据
        List<Projects> projects = projectMapper.selectByCondition(query);
        int total = projectMapper.countByCondition(query);

        // 4. 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("list", projects);
        result.put("total", total);
        result.put("pageNum", (offset / query.getPageSize()) + 1);
        result.put("pageSize", query.getPageSize());

        return result;
    }

    @Override
    @Transactional
    public void updateProjectPhase(UpdateProjectPhaseDTO dto) {

        // 1. 参数校验
        if (dto.getProjectId() == null) {
            throw new BizException("项目ID不能为空");
        }
        if (dto.getNewPhase() == null) {
            throw new BizException("新阶段不能为空");
        }

        // 2. 校验项目是否存在
        Projects project = projectMapper.selectById(dto.getProjectId());
        if (project == null) {
            throw new BizException("项目不存在");
        }

        // 3. 校验新阶段合法性
        ProjectPhase newPhase;
        try {
            newPhase = ProjectPhase.fromCode(dto.getNewPhase());
        } catch (IllegalArgumentException e) {
            throw new BizException("非法的项目阶段");
        }

        // 4. 阶段变更业务规则校验（可选）
        ProjectPhase currentPhase = project.getProjectPhase();
        if (currentPhase == ProjectPhase.ARCHIVED) {
            throw new BizException("已归档的项目不能修改阶段");
        }

        // 5. 更新阶段
        projectMapper.updatePhase(dto.getProjectId(), dto.getNewPhase());
    }

    @Override
    @Transactional
    public void deleteProject(Long projectId) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }

        Projects project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BizException("项目不存在");
        }

        // 删除项目（注意：级联删除会删除关联的资产）
        projectMapper.deleteById(projectId);
    }
}
