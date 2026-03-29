package com.xie.platform.access.policy.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.environment.Environment;
import com.xie.platform.access.policy.Policy;
import com.xie.platform.access.policy.PolicyLayer;
import com.xie.platform.access.policy.PolicyResult;
import com.xie.platform.access.policy.support.AssetTypeOwnershipRules;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.access.resource.ResourceType;
import com.xie.platform.access.subject.Subject;
import com.xie.platform.model.enumValue.AssetType;
import com.xie.platform.model.enumValue.DeptType;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AssetTypeOwnershipPolicy implements Policy {

    @Override
    public String getName() {
        return "AssetTypeOwnershipPolicy";
    }

    @Override
    public PolicyLayer getLayer() {
        return PolicyLayer.PROJECT;
    }

    @Override
    public int getOrder() {
        return 25;
    }

    @Override
    public PolicyResult evaluate(Subject subject, Resource resource, Action action, Environment environment) {
        if (resource.getType() != ResourceType.ASSET || action != Action.WRITE) {
            return PolicyResult.ALLOW;
        }

        DeptType deptType = subject.getDeptType();
        AssetType assetType = resource.getAssetType();
        if (deptType == null || assetType == null) {
            return PolicyResult.DENY;
        }

        Set<DeptType> allowedWriterDepts = AssetTypeOwnershipRules.getAllowedWriterDepts(assetType);
        return allowedWriterDepts.contains(deptType) ? PolicyResult.ALLOW : PolicyResult.DENY;
    }
}
