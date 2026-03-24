package com.xie.platform.access.policy.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.environment.Environment;
import com.xie.platform.access.policy.Policy;
import com.xie.platform.access.policy.PolicyLayer;
import com.xie.platform.access.policy.PolicyResult;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.access.subject.Subject;
import com.xie.platform.model.enumValue.DeptType;
import com.xie.platform.model.enumValue.ProjectPhase;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * 策略一：项目阶段访问控制
 *
 * 根据资源所处的项目阶段，判断主体所在部门是否有权限执行本次操作。
 * 规则来源：业务场景文档"项目阶段访问控制矩阵"。
 *
 * 裁决逻辑：
 *   - 归档阶段：只有管理层可以访问，且仅限 READ；其余一律拒绝。
 *   - 管理层（非归档）：拥有全阶段 READ 权限（监管职能）；不参与业务写操作。
 *   - 其他部门：必须在当前阶段的"可访问部门"列表内，才允许任意操作。
 *
 * 优先级说明：
 *   本策略只负责阶段维度的判断，不处理密级与职级限制（由 SecurityLevelPolicy 负责）。
 */
@Component
public class PhaseAccessPolicy implements Policy {

    @Override
    public String getName() {
        return "PhaseAccessPolicy";
    }

    @Override
    public PolicyLayer getLayer() {
        return PolicyLayer.PROJECT;
    }

    @Override
    public PolicyResult evaluate(Subject subject, Resource resource, Action action, Environment environment) {
        ProjectPhase phase = resource.getProjectPhase();
        DeptType deptType = subject.getDeptType();

        // 资源未关联项目阶段，或主体部门信息缺失，本条规则不介入，直接放行
        if (phase == null || deptType == null) {
            return PolicyResult.DENY;
        }

        // ── 归档阶段 ──────────────────────────────────────────────────────────
        if (phase == ProjectPhase.ARCHIVED) {
            if (deptType != DeptType.MANAGEMENT) {
                return PolicyResult.DENY;
            }
            return action == Action.READ ? PolicyResult.ALLOW : PolicyResult.DENY;
        }

        // ── 非归档阶段：先按矩阵检查完整操作权 ──────────────────────────────
        Set<DeptType> fullAccessDepts = getAllowedDepts(phase);
        if (fullAccessDepts.contains(deptType)) {
            return PolicyResult.ALLOW;
        }

        // ── 管理层兜底监管权 ─────────────────────────────────────────────────
        if (deptType == DeptType.MANAGEMENT) {
            return action == Action.READ ? PolicyResult.ALLOW : PolicyResult.DENY;
        }

        return PolicyResult.DENY;
    }

    /**
     * 返回指定阶段拥有完整操作权限（增删改查）的部门集合。
     * 依据：业务文档"项目阶段访问控制矩阵"中"可访问部门"列。
     *
     * 注意：管理层在立项阶段作为业务参与方列入此处；
     * 其余阶段的管理层监管权（只读）由上层逻辑单独处理。
     */
    private Set<DeptType> getAllowedDepts(ProjectPhase phase) {
        switch (phase) {
            case INIT:        return EnumSet.of(DeptType.PRODUCT, DeptType.MANAGEMENT);
            case REQUIREMENT: return EnumSet.of(DeptType.PRODUCT, DeptType.RD);
            case DEVELOPMENT: return EnumSet.of(DeptType.RD,      DeptType.PRODUCT);
            case TEST:        return EnumSet.of(DeptType.QA,       DeptType.RD);
            case RELEASE:     return EnumSet.of(DeptType.OPS,      DeptType.RD);
            default:          return EnumSet.noneOf(DeptType.class);
        }
    }
}
