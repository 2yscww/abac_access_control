package com.xie.platform.access.pdp;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.environment.Environment;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.access.subject.Subject;

/**
 * 策略决策点接口（PDP）
 * 接收四要素，返回最终访问决策。
 */
public interface PolicyDecisionPoint {

    DecisionResult evaluate(Subject subject, Resource resource, Action action, Environment environment);
}
