package com.xie.platform.mapper;

import com.xie.platform.dto.ProjectQueryDTO;
import com.xie.platform.model.Projects;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProjectMapper {

    int insert(Projects project);

    Projects selectById(@Param("projectId") Long projectId);

    Projects selectByName(@Param("projectName") String projectName);

    List<Projects> selectByCondition(ProjectQueryDTO query);

    int countByCondition(ProjectQueryDTO query);

    List<Projects> selectByPhaseCodes(@Param("phaseCodes") List<Integer> phaseCodes);

    int updatePhase(
            @Param("projectId") Long projectId,
            @Param("newPhase") Integer newPhase,
            @Param("ownerId") Long ownerId
    );

    int updateOwnerByPhaseCodes(
            @Param("phaseCodes") List<Integer> phaseCodes,
            @Param("ownerId") Long ownerId
    );

    int deleteById(@Param("projectId") Long projectId);
}
