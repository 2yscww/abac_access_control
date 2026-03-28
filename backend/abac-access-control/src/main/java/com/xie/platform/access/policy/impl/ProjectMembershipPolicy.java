package com.xie.platform.access.policy.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.environment.Environment;
import com.xie.platform.access.policy.Policy;
import com.xie.platform.access.policy.PolicyLayer;
import com.xie.platform.access.policy.PolicyResult;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.access.subject.Subject;
import com.xie.platform.model.enumValue.DeptType;
import com.xie.platform.service.ProjectMemberService;
import org.springframework.stereotype.Component;

@Component
public class ProjectMembershipPolicy implements Policy {

    private final ProjectMemberService projectMemberService;

    public ProjectMembershipPolicy(ProjectMemberService projectMemberService) {
        this.projectMemberService = projectMemberService;
    }

    @Override
    public String getName() {
        return "ProjectMembershipPolicy";
    }

    @Override
    public PolicyLayer getLayer() {
        return PolicyLayer.PROJECT;
    }

    @Override
    public int getOrder() {
        return 10;
    }

    private boolean isManagementReadLikeAccess(Subject subject, Action action) {
        return subject.getDeptType() == DeptType.MANAGEMENT
                && (action == Action.READ || action == Action.EXPORT);
    }

    @Override
    public PolicyResult evaluate(Subject subject, Resource resource, Action action, Environment environment) {
        if (resource.getProjectId() == null) {
            return PolicyResult.ALLOW;
        }
        if (action == Action.ADVANCE_PHASE) {
            return PolicyResult.ALLOW;
        }
        if (subject.getEmployeeId() == null) {
            return PolicyResult.DENY;
        }
        if (isManagementReadLikeAccess(subject, action)) {
            return PolicyResult.ALLOW;
        }

        return projectMemberService.isActiveMember(resource.getProjectId(), subject.getEmployeeId())
                ? PolicyResult.ALLOW
                : PolicyResult.DENY;
    }
}
