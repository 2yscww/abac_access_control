package com.xie.platform.service;

import com.xie.platform.dto.ProjectMemberDTO;
import com.xie.platform.model.enumValue.ProjectPhase;

import java.util.List;

public interface ProjectMemberService {

    boolean isActiveMember(Long projectId, Long employeeId);

    void initializeProjectMembers(Long projectId, Long creatorEmployeeId, Long ownerId, ProjectPhase projectPhase);

    void syncMembersForPhaseTransition(
            Long projectId,
            ProjectPhase currentPhase,
            ProjectPhase newPhase,
            Long nextOwnerId,
            Long operatorEmployeeId
    );

    void ensureProjectOwnerMembership(Long projectId, Long ownerId, ProjectPhase projectPhase);

    List<ProjectMemberDTO> listProjectMembers(Long projectId, Long operatorEmployeeId);

    void addProjectMember(Long projectId, Long employeeId, Long operatorEmployeeId);

    void removeProjectMember(Long projectId, Long employeeId, Long operatorEmployeeId);

    void deactivateByEmployeeId(Long employeeId);

    void deleteByProjectId(Long projectId);
}
