package com.xie.platform.access.policy.support;

import com.xie.platform.model.enumValue.AssetType;
import com.xie.platform.model.enumValue.DeptType;

import java.util.EnumSet;
import java.util.Set;

public final class AssetTypeOwnershipRules {

    private AssetTypeOwnershipRules() {
    }

    public static Set<DeptType> getAllowedWriterDepts(AssetType assetType) {
        if (assetType == null) {
            return EnumSet.noneOf(DeptType.class);
        }

        return switch (assetType) {
            case REQUIREMENT_DOC -> EnumSet.of(DeptType.PRODUCT);
            case DESIGN_DOC, SOURCE_CODE -> EnumSet.of(DeptType.RD);
            case TEST_REPORT -> EnumSet.of(DeptType.QA);
            case DEPLOY_SCRIPT, OPS_DOC -> EnumSet.of(DeptType.OPS);
        };
    }
}
