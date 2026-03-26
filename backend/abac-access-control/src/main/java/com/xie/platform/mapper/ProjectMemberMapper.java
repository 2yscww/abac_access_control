package com.xie.platform.mapper;

import com.xie.platform.dto.ProjectMemberDTO;
import com.xie.platform.model.ProjectMember;
import com.xie.platform.model.enumValue.ProjectPhase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProjectMemberMapper {

    ProjectMember selectByProjectIdAndEmployeeId(
            @Param("projectId") Long projectId,
            @Param("employeeId") Long employeeId
    );

    int countActiveMember(
            @Param("projectId") Long projectId,
            @Param("employeeId") Long employeeId
    );

    int insert(ProjectMember projectMember);

    int reactivate(
            @Param("projectId") Long projectId,
            @Param("employeeId") Long employeeId,
            @Param("joinedPhase") ProjectPhase joinedPhase
    );

    int deactivate(
            @Param("projectId") Long projectId,
            @Param("employeeId") Long employeeId
    );

    int deactivateByEmployeeId(@Param("employeeId") Long employeeId);

    List<ProjectMemberDTO> selectByProjectId(@Param("projectId") Long projectId);

    List<ProjectMemberDTO> selectActiveByProjectId(@Param("projectId") Long projectId);

    int deleteByProjectId(@Param("projectId") Long projectId);
}
