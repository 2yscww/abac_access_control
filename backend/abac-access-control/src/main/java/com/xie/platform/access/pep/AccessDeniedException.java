package com.xie.platform.access.pep;

/**
 * 访问拒绝异常
 *
 * 当 PEP 检测到访问被拒绝时抛出此异常
 * 包含拒绝原因和触发的策略名称，便于审计和用户提示
 */
public class AccessDeniedException extends RuntimeException {

    /** 触发拒绝的策略名称 */
    private final String policyName;

    /** 拒绝原因 */
    private final String reason;

    public AccessDeniedException(String policyName, String reason) {
        super(reason);
        this.policyName = policyName;
        this.reason = reason;
    }

    public String getPolicyName() {
        return policyName;
    }

    public String getReason() {
        return reason;
    }
}
