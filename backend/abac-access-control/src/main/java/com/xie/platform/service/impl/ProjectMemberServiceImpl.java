package com.xie.platform.service.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.policy.support.ProjectPhaseAccessRules;
import com.xie.platform.dto.ProjectMemberDTO;
import com.xie.platform.exception.BizException;
import com.xie.platform.mapper.DepartmentMapper;
import com.xie.platform.mapper.EmployeesMapper;
import com.xie.platform.mapper.ProjectMapper;
import com.xie.platform.mapper.ProjectMemberMapper;
import com.xie.platform.model.Department;
import com.xie.platform.model.Employees;
import com.xie.platform.model.ProjectMember;
import com.xie.platform.model.Projects;
import com.xie.platform.model.enumValue.DeptType;
import com.xie.platform.model.enumValue.EmployeeStatus;
import com.xie.platform.model.enumValue.ProjectMemberStatus;
import com.xie.platform.model.enumValue.ProjectPhase;
import com.xie.platform.service.AuditLogService;
import com.xie.platform.service.ProjectMemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ProjectMemberServiceImpl implements ProjectMemberService {

    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectMapper projectMapper;
    private final EmployeesMapper employeesMapper;
    private final DepartmentMapper departmentMapper;
    private final AuditLogService auditLogService;

    public ProjectMemberServiceImpl(
            ProjectMemberMapper projectMemberMapper,
            ProjectMapper projectMapper,
            EmployeesMapper employeesMapper,
            DepartmentMapper departmentMapper,
            AuditLogService auditLogService) {
        this.projectMemberMapper = projectMemberMapper;
        this.projectMapper = projectMapper;
        this.employeesMapper = employeesMapper;
        this.departmentMapper = departmentMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActiveMember(Long projectId, Long employeeId) {
        if (projectId == null || employeeId == null) {
            return false;
        }
        return projectMemberMapper.countActiveMember(projectId, employeeId) > 0;
    }

    @Override
    @Transactional
    public void initializeProjectMembers(Long projectId, Long creatorEmployeeId, Long ownerId, ProjectPhase projectPhase) {
        ensureActiveMembership(projectId, creatorEmployeeId, projectPhase);
        ensureActiveMembership(projectId, ownerId, projectPhase);
    }

    @Override
    @Transactional
    public void syncMembersForPhaseTransition(
            Long projectId,
            ProjectPhase currentPhase,
            ProjectPhase newPhase,
            Long nextOwnerId,
            Long operatorEmployeeId) {
        if (projectId == null || currentPhase == null || newPhase == null) {
            throw new BizException("项目成员同步缺少必要参数");
        }

        List<ProjectMemberDTO> activeMembers = projectMemberMapper.selectActiveByProjectId(projectId);
        Set<DeptType> allowedDepts = ProjectPhaseAccessRules.getAllowedDepts(newPhase);

        for (ProjectMemberDTO member : activeMembers) {
            if (shouldKeepMembership(member, allowedDepts, newPhase, nextOwnerId)) {
                continue;
            }
            projectMemberMapper.deactivate(projectId, member.getEmployeeId());
            auditLogService.recordBusinessEvent(
                    operatorEmployeeId,
                    "PROJECT",
                    projectId,
                    Action.AUTO_REMOVE_PROJECT_MEMBER,
                    buildAutoRemovalDetail(projectId, currentPhase, newPhase, member, operatorEmployeeId)
            );
        }

        if (newPhase != ProjectPhase.ARCHIVED) {
            ensureActiveMembership(projectId, nextOwnerId, newPhase);
        }
    }

    @Override
    @Transactional
    public void ensureProjectOwnerMembership(Long projectId, Long ownerId, ProjectPhase projectPhase) {
        if (projectPhase == ProjectPhase.ARCHIVED) {
            return;
        }
        ensureActiveMembership(projectId, ownerId, projectPhase);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMemberDTO> listProjectMembers(Long projectId, Long operatorEmployeeId) {
        ensureMembershipOperator(projectId, operatorEmployeeId);
        return projectMemberMapper.selectByProjectId(projectId);
    }

    @Override
    @Transactional
    public void addProjectMember(Long projectId, Long employeeId, Long operatorEmployeeId) {
        Projects project = ensureMembershipOperator(projectId, operatorEmployeeId);
        ensureProjectCanManageMembers(project);
        Employees targetEmployee = validateActiveEmployee(employeeId, "目标员工不存在");
        Department targetDepartment = validateDepartment(targetEmployee.getDeptId(), "目标员工所属部门不存在");

        if (!ProjectPhaseAccessRules.getAllowedDepts(project.getProjectPhase()).contains(targetDepartment.getDeptType())) {
            throw new BizException("目标员工所属部门不在当前项目阶段允许范围内");
        }
        if (isActiveMember(projectId, employeeId)) {
            throw new BizException("目标员工已是当前项目成员");
        }

        ensureActiveMembership(projectId, employeeId, project.getProjectPhase());

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("operatorEmployeeId", operatorEmployeeId);
        detail.put("projectId", projectId);
        detail.put("projectName", project.getProjectName());
        detail.put("projectPhase", project.getProjectPhase() != null ? project.getProjectPhase().name() : null);
        detail.put("targetEmployeeId", targetEmployee.getEmployeeId());
        detail.put("targetEmployeeCode", targetEmployee.getEmployeeCode());
        detail.put("targetEmployeeName", targetEmployee.getEmployeeName());
        detail.put("targetDeptId", targetDepartment.getDeptId());
        detail.put("targetDeptType", targetDepartment.getDeptType() != null ? targetDepartment.getDeptType().name() : null);

        auditLogService.recordBusinessEvent(
                operatorEmployeeId,
                "PROJECT",
                projectId,
                Action.ADD_PROJECT_MEMBER,
                detail
        );
    }

    @Override
    @Transactional
    public void removeProjectMember(Long projectId, Long employeeId, Long operatorEmployeeId) {
        Projects project = ensureMembershipOperator(projectId, operatorEmployeeId);
        ensureProjectCanManageMembers(project);

        if (project.getOwnerId() != null && project.getOwnerId().equals(employeeId)) {
            throw new BizException("当前阶段负责人不能直接移出项目成员");
        }

        ProjectMember existingMember = projectMemberMapper.selectByProjectIdAndEmployeeId(projectId, employeeId);
        if (existingMember == null || existingMember.getStatus() != ProjectMemberStatus.ACTIVE) {
            throw new BizException("目标员工不是当前项目有效成员");
        }

        Employees targetEmployee = employeesMapper.selectByEmployeeId(employeeId);
        projectMemberMapper.deactivate(projectId, employeeId);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("operatorEmployeeId", operatorEmployeeId);
        detail.put("projectId", projectId);
        detail.put("projectName", project.getProjectName());
        detail.put("projectPhase", project.getProjectPhase() != null ? project.getProjectPhase().name() : null);
        detail.put("targetEmployeeId", employeeId);
        detail.put("targetEmployeeCode", targetEmployee != null ? targetEmployee.getEmployeeCode() : null);
        detail.put("targetEmployeeName", targetEmployee != null ? targetEmployee.getEmployeeName() : null);

        auditLogService.recordBusinessEvent(
                operatorEmployeeId,
                "PROJECT",
                projectId,
                Action.REMOVE_PROJECT_MEMBER,
                detail
        );
    }

    @Override
    @Transactional
    public void deactivateByEmployeeId(Long employeeId) {
        if (employeeId == null) {
            return;
        }
        projectMemberMapper.deactivateByEmployeeId(employeeId);
    }

    @Override
    @Transactional
    public void deleteByProjectId(Long projectId) {
        if (projectId == null) {
            return;
        }
        projectMemberMapper.deleteByProjectId(projectId);
    }

    private boolean shouldKeepMembership(
            ProjectMemberDTO member,
            Set<DeptType> allowedDepts,
            ProjectPhase newPhase,
            Long nextOwnerId) {
        if (member == null || member.getEmployeeId() == null) {
            return false;
        }
        if (newPhase == ProjectPhase.ARCHIVED) {
            return false;
        }
        if (member.getEmployeeId().equals(nextOwnerId)) {
            return true;
        }
        return member.getDeptType() != null && allowedDepts.contains(member.getDeptType());
    }

    private void ensureActiveMembership(Long projectId, Long employeeId, ProjectPhase projectPhase) {
        if (projectId == null || employeeId == null || projectPhase == null) {
            return;
        }

        ProjectMember existingMember = projectMemberMapper.selectByProjectIdAndEmployeeId(projectId, employeeId);
        if (existingMember == null) {
            ProjectMember projectMember = new ProjectMember();
            projectMember.setProjectId(projectId);
            projectMember.setEmployeeId(employeeId);
            projectMember.setStatus(ProjectMemberStatus.ACTIVE);
            projectMember.setJoinedPhase(projectPhase);
            projectMember.setJoinedAt(LocalDateTime.now());
            projectMember.setLeftAt(null);
            projectMemberMapper.insert(projectMember);
            return;
        }

        if (existingMember.getStatus() == ProjectMemberStatus.ACTIVE) {
            return;
        }

        projectMemberMapper.reactivate(projectId, employeeId, projectPhase);
    }

    private Projects ensureMembershipOperator(Long projectId, Long operatorEmployeeId) {
        Projects project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BizException("项目不存在");
        }

        Employees operator = validateActiveEmployee(operatorEmployeeId, "当前操作人不存在");
        Department operatorDepartment = validateDepartment(operator.getDeptId(), "当前操作人所属部门不存在");
        if (operatorDepartment.getDeptType() == DeptType.MANAGEMENT) {
            return project;
        }
        if (project.getOwnerId() != null && project.getOwnerId().equals(operatorEmployeeId)) {
            return project;
        }

        throw new BizException("仅当前阶段负责人或管理层允许维护项目成员");
    }

    private void ensureProjectCanManageMembers(Projects project) {
        if (project.getProjectPhase() == ProjectPhase.ARCHIVED) {
            throw new BizException("归档项目不允许再维护项目成员");
        }
    }

    private Employees validateActiveEmployee(Long employeeId, String notFoundMessage) {
        Employees employee = employeesMapper.selectByEmployeeId(employeeId);
        if (employee == null) {
            throw new BizException(notFoundMessage);
        }
        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new BizException("目标员工状态不可用");
        }
        return employee;
    }

    private Department validateDepartment(Long deptId, String notFoundMessage) {
        Department department = departmentMapper.selectById(deptId);
        if (department == null) {
            throw new BizException(notFoundMessage);
        }
        return department;
    }

    private Map<String, Object> buildAutoRemovalDetail(
            Long projectId,
            ProjectPhase currentPhase,
            ProjectPhase newPhase,
            ProjectMemberDTO member,
            Long operatorEmployeeId) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("operatorEmployeeId", operatorEmployeeId);
        detail.put("projectId", projectId);
        detail.put("fromPhase", currentPhase != null ? currentPhase.name() : null);
        detail.put("toPhase", newPhase != null ? newPhase.name() : null);
        detail.put("targetEmployeeId", member.getEmployeeId());
        detail.put("targetEmployeeCode", member.getEmployeeCode());
        detail.put("targetEmployeeName", member.getEmployeeName());
        detail.put("targetDeptType", member.getDeptType() != null ? member.getDeptType().name() : null);
        detail.put("reason", "PHASE_TRANSITION_AUTO_CLEANUP");
        return detail;
    }
}
