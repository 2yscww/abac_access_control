package com.xie.platform.access.policy;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.environment.Environment;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.access.subject.Subject;

/**
 * ABAC 策略接口
 *
 * 每一条业务规则都实现这个接口。
 * PDP 按 PolicyLayer 分层收集所有 Policy，由高到低依次评估。
 */
public interface Policy {

    /**
     * 评估本条规则
     *
     * @return ALLOW      — 通过，继续交由下一层判断
     *         DENY       — 拒绝，PDP 立即终止
     *         FORCE_ALLOW— 强制放行，跳过所有后续层
     */
    PolicyResult evaluate(Subject subject, Resource resource, Action action, Environment environment);

    /**
     * 规则名称，用于审计日志记录是哪条规则触发了决策
     */
    String getName();

    /**
     * 规则所属的优先级层，PDP 据此分组并按序执行
     */
    PolicyLayer getLayer();
}
