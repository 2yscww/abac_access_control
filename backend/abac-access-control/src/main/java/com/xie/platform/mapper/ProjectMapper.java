package com.xie.platform.mapper;

import com.xie.platform.dto.ProjectQueryDTO;
import com.xie.platform.model.Projects;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProjectMapper {

    /**
     * 插入项目
     */
    int insert(Projects project);

    /**
     * 根据项目ID查询
     */
    Projects selectById(@Param("projectId") Long projectId);

    /**
     * 根据项目名称查询（用于唯一性校验）
     */
    Projects selectByName(@Param("projectName") String projectName);

    /**
     * 条件查询项目列表（分页）
     */
    List<Projects> selectByCondition(ProjectQueryDTO query);

    /**
     * 统计符合条件的项目数量
     */
    int countByCondition(ProjectQueryDTO query);

    /**
     * 更新项目阶段
     */
    int updatePhase(@Param("projectId") Long projectId, @Param("newPhase") Integer newPhase);

    /**
     * 删除项目（物理删除）
     */
    int deleteById(@Param("projectId") Long projectId);
}
