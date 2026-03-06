package com.xie.platform.access.pdp;

import lombok.Getter;

/**
 * PDP 的最终决策结果
 *
 * 除了 allowed 标志之外，还记录了触发决策的规则名称，
 * 供审计日志使用，便于追溯"是哪条规则拒绝了这次访问"。
 */
@Getter
public class DecisionResult {

    /** true = 允许访问，false = 拒绝访问 */
    private final boolean allowed;

    /** 触发本次决策的规则名称（DENY 时是拒绝的规则，ALLOW 时为 "default-allow"） */
    private final String triggerPolicy;

    /** 人类可读的决策原因，用于日志和前端提示 */
    private final String reason;

    private DecisionResult(boolean allowed, String triggerPolicy, String reason) {
        this.allowed = allowed;
        this.triggerPolicy = triggerPolicy;
        this.reason = reason;
    }

    public static DecisionResult allow() {
        return new DecisionResult(true, "default-allow", "所有策略通过，允许访问");
    }

    public static DecisionResult forceAllow(String policyName) {
        return new DecisionResult(true, policyName, "安全策略强制放行");
    }

    public static DecisionResult deny(String policyName, String reason) {
        return new DecisionResult(false, policyName, reason);
    }
}
