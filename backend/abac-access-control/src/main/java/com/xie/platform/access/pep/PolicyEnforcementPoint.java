package com.xie.platform.access.pep;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.pdp.DecisionResult;
import com.xie.platform.access.resource.Resource;

public interface PolicyEnforcementPoint {

    DecisionResult decideAccess(Long employeeId, Resource resource, Action action);

    void checkAccess(Long employeeId, Resource resource, Action action);

    DecisionResult decideProjectAccess(Long employeeId, Long projectId, Action action);

    DecisionResult checkProjectAccess(Long employeeId, Long projectId, Action action);

    DecisionResult decideAssetAccess(Long employeeId, Long assetId, Action action);

    DecisionResult checkAssetAccess(Long employeeId, Long assetId, Action action);
}
