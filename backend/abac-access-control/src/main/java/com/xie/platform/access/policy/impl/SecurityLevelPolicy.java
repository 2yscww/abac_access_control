package com.xie.platform.access.policy.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.environment.Environment;
import com.xie.platform.access.policy.Policy;
import com.xie.platform.access.policy.PolicyLayer;
import com.xie.platform.access.policy.PolicyResult;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.access.subject.Subject;
import com.xie.platform.model.enumValue.SecurityLevel;
import org.springframework.stereotype.Component;


// TODO 还是要继续修改策略的设置，需要执行 need to know 
// ? 就算是高级负责人，也不一定是负责研发工作，不能因为他级别高，就能随意查看所有源代码，反之，研发工程师同理

/**
 * 策略二：资源密级与员工职级匹配（安全策略层）
 *
 * 根据资源的保密等级，判断主体的职级是否达到访问门槛。
 * 同时对外包人员实施额外限制：外包人员不允许访问机密及以上级别的资源。
 *
 * 密级与最低职级的映射关系：
 *   PUBLIC      (1) → P1+（所有员工）
 *   INTERNAL    (2) → P3+
 *   CONFIDENTIAL(3) → P5+，且非外包
 *   TOP_SECRET  (4) → VP+，且非外包
 *
 * 说明：
 *   本策略不区分 Action，密级门槛对读写删均一致。
 *   能否对某密级资源执行写操作，由 PhaseAccessPolicy 从阶段角度进一步限制。
 */
@Component
public class SecurityLevelPolicy implements Policy {

    @Override
    public String getName() {
        return "SecurityLevelPolicy";
    }

    @Override
    public PolicyLayer getLayer() {
        return PolicyLayer.SECURITY;
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public PolicyResult evaluate(Subject subject, Resource resource, Action action, Environment environment) {
        SecurityLevel securityLevel = resource.getSecurityLevel();
        if (securityLevel == null || subject.getLevel() == null) {
            return PolicyResult.ALLOW; // 信息不足，本条规则不介入
        }

        // ── 外包人员限制 ──────────────────────────────────────────────────────
        // 外包人员属于非正式员工，无论职级高低，一律不得访问机密及以上级别的资源
        if (Boolean.TRUE.equals(subject.getIsContractor())) {
            if (securityLevel.getLevel() >= SecurityLevel.CONFIDENTIAL.getLevel()) {
                return PolicyResult.DENY;
            }
        }

        // ── 职级门槛检查 ──────────────────────────────────────────────────────
        // 员工的职级 rank 必须 >= 当前密级要求的最低 rank，否则拒绝
        int employeeRank = subject.getLevel().getRank();
        int requiredRank = getRequiredRank(securityLevel);
        return employeeRank >= requiredRank ? PolicyResult.ALLOW : PolicyResult.DENY;
    }

    /**
     * 返回访问指定密级资源所需的最低员工职级 rank。
     *
     * PUBLIC      → P1  (rank 1)
     * INTERNAL    → P3  (rank 3)
     * CONFIDENTIAL→ P5  (rank 5)
     * TOP_SECRET  → VP  (rank 9)
     */
    private int getRequiredRank(SecurityLevel securityLevel) {
        switch (securityLevel) {
            case PUBLIC:       return 1;
            case INTERNAL:     return 3;
            case CONFIDENTIAL: return 5;
            case TOP_SECRET:   return 9;
            default:           return Integer.MAX_VALUE; // 未知密级，拒绝所有人
        }
    }
}
