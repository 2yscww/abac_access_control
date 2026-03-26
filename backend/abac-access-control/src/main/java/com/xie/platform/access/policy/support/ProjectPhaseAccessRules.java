package com.xie.platform.access.policy.support;

import com.xie.platform.model.enumValue.DeptType;
import com.xie.platform.model.enumValue.ProjectPhase;

import java.util.EnumSet;
import java.util.Set;

public final class ProjectPhaseAccessRules {

    private ProjectPhaseAccessRules() {
    }

    public static Set<DeptType> getAllowedDepts(ProjectPhase phase) {
        if (phase == null) {
            return EnumSet.noneOf(DeptType.class);
        }

        return switch (phase) {
            case INIT -> EnumSet.of(DeptType.PRODUCT, DeptType.MANAGEMENT);
            case REQUIREMENT -> EnumSet.of(DeptType.PRODUCT, DeptType.RD);
            case DEVELOPMENT -> EnumSet.of(DeptType.RD, DeptType.PRODUCT);
            case TEST -> EnumSet.of(DeptType.QA, DeptType.RD);
            case RELEASE -> EnumSet.of(DeptType.OPS, DeptType.RD);
            case ARCHIVED -> EnumSet.noneOf(DeptType.class);
        };
    }
}
