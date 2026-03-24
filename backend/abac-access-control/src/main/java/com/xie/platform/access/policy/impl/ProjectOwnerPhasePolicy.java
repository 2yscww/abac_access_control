package com.xie.platform.access.policy.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.environment.Environment;
import com.xie.platform.access.policy.Policy;
import com.xie.platform.access.policy.PolicyLayer;
import com.xie.platform.access.policy.PolicyResult;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.access.resource.ResourceType;
import com.xie.platform.access.subject.Subject;
import com.xie.platform.mapper.DepartmentMapper;
import com.xie.platform.model.Department;
import com.xie.platform.model.enumValue.DeptType;
import com.xie.platform.model.enumValue.ProjectPhase;
import org.springframework.stereotype.Component;

/**
 * Restricts project phase advancement to the configured manager of the
 * department that owns the current phase.
 */
@Component
public class ProjectOwnerPhasePolicy implements Policy {

    private final DepartmentMapper departmentMapper;

    public ProjectOwnerPhasePolicy(DepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
    }

    @Override
    public String getName() {
        return "ProjectOwnerPhasePolicy";
    }

    @Override
    public PolicyLayer getLayer() {
        return PolicyLayer.ROLE;
    }

    @Override
    public PolicyResult evaluate(Subject subject, Resource resource, Action action, Environment environment) {
        if (resource.getType() != ResourceType.PROJECT || action != Action.ADVANCE_PHASE) {
            return PolicyResult.ALLOW;
        }
        if (subject.getEmployeeId() == null
                || subject.getDeptId() == null
                || subject.getDeptType() == null
                || resource.getProjectPhase() == null) {
            return PolicyResult.DENY;
        }

        DeptType expectedDeptType = getResponsibleDept(resource.getProjectPhase());
        if (subject.getDeptType() != expectedDeptType) {
            return PolicyResult.DENY;
        }

        Department department = departmentMapper.selectById(subject.getDeptId());
        if (department == null || department.getManagerId() == null) {
            return PolicyResult.DENY;
        }

        return subject.getEmployeeId().equals(department.getManagerId())
                ? PolicyResult.ALLOW
                : PolicyResult.DENY;
    }

    private DeptType getResponsibleDept(ProjectPhase projectPhase) {
        return switch (projectPhase) {
            case INIT -> DeptType.MANAGEMENT;
            case REQUIREMENT -> DeptType.PRODUCT;
            case DEVELOPMENT -> DeptType.RD;
            case TEST -> DeptType.QA;
            case RELEASE -> DeptType.OPS;
            case ARCHIVED -> DeptType.MANAGEMENT;
        };
    }
}
