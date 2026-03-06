package com.xie.platform.access.policy;

/**
 * 单条策略的评估结果
 *
 * ALLOW      — 本条规则通过，交由下一层继续判断
 * DENY       — 本条规则明确拒绝，PDP 立即终止并返回拒绝
 * FORCE_ALLOW— 强制放行，PDP 跳过所有后续层直接允许（为临时授权/例外授权预留）
 */
public enum PolicyResult {
    ALLOW,
    DENY,
    FORCE_ALLOW
}
