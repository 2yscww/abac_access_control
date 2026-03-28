package com.xie.platform.access.policy.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.environment.Environment;
import com.xie.platform.access.policy.Policy;
import com.xie.platform.access.policy.PolicyLayer;
import com.xie.platform.access.policy.PolicyResult;
import com.xie.platform.access.policy.support.ProjectPhaseAccessRules;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.access.resource.ResourceType;
import com.xie.platform.access.subject.Subject;
import com.xie.platform.model.enumValue.DeptType;
import com.xie.platform.model.enumValue.ProjectPhase;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 资产历史阶段访问控制。
 *
 * <p>项目当前阶段描述“项目现在走到哪里”，资产产生阶段描述“这个资产最初属于哪个阶段的材料”。
 * 例如项目已经进入研发阶段，但立项阶段形成的预算、审批、评估材料不应自动对研发开放。</p>
 */
@Component
public class AssetStageAccessPolicy implements Policy {

    private static boolean isReadLikeAction(Action action) {
        return action == Action.READ || action == Action.EXPORT;
    }

    @Override
    public String getName() {
        return "AssetStageAccessPolicy";
    }

    @Override
    public PolicyLayer getLayer() {
        return PolicyLayer.PROJECT;
    }

    @Override
    public int getOrder() {
        return 30;
    }

    @Override
    public PolicyResult evaluate(Subject subject, Resource resource, Action action, Environment environment) {
        if (resource.getType() != ResourceType.ASSET) {
            return PolicyResult.ALLOW;
        }

        ProjectPhase assetsStage = resource.getAssetsStage();
        DeptType deptType = subject.getDeptType();
        if (assetsStage == null || deptType == null) {
            return PolicyResult.DENY;
        }

        if (assetsStage == ProjectPhase.ARCHIVED) {
            if (deptType != DeptType.MANAGEMENT) {
                return PolicyResult.DENY;
            }
            return isReadLikeAction(action) ? PolicyResult.ALLOW : PolicyResult.DENY;
        }

        Set<DeptType> fullAccessDepts = getAllowedDepts(assetsStage);
        if (fullAccessDepts.contains(deptType)) {
            return PolicyResult.ALLOW;
        }

        if (deptType == DeptType.MANAGEMENT) {
            return isReadLikeAction(action) ? PolicyResult.ALLOW : PolicyResult.DENY;
        }

        return PolicyResult.DENY;
    }

    private Set<DeptType> getAllowedDepts(ProjectPhase assetsStage) {
        return ProjectPhaseAccessRules.getAllowedDepts(assetsStage);
    }
}
