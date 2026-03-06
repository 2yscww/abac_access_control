package com.xie.platform.access.pdp;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.environment.Environment;
import com.xie.platform.access.policy.Policy;
import com.xie.platform.access.policy.PolicyLayer;
import com.xie.platform.access.policy.PolicyResult;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.access.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ABAC 策略决策点实现
 *
 * 按优先级分层执行所有策略：安全策略层 → 项目策略层 → 角色属性层
 * 每层内部任一规则返回 DENY 即终止，返回 FORCE_ALLOW 则跳过所有后续层。
 * 所有层全部通过后，默认允许。
 *
 * 新增规则只需实现 Policy 接口并加 @Component，无需修改本类。
 */
@Component
public class AbacPolicyDecisionPoint implements PolicyDecisionPoint {

    /**
     * Spring 自动收集所有实现了 Policy 接口的 Bean
     * 每个 Policy 通过 getLayer() 声明自己属于哪一层
     */
    @Autowired
    private List<Policy> allPolicies;

    /** 按层分组后的规则集合，PostConstruct 时初始化一次 */
    private Map<PolicyLayer, List<Policy>> policyMap;

    /**
     * 按优先级定义的层执行顺序
     * 调整这里的顺序即可改变层间优先级，无需改动任何规则类
     */
    private static final PolicyLayer[] LAYER_ORDER = {
            PolicyLayer.SECURITY,
            PolicyLayer.PROJECT,
            PolicyLayer.ROLE
    };

    @PostConstruct
    public void init() {
        policyMap = allPolicies.stream()
                .collect(Collectors.groupingBy(Policy::getLayer));
    }

    @Override
    public DecisionResult evaluate(Subject subject, Resource resource, Action action, Environment environment) {

        for (PolicyLayer layer : LAYER_ORDER) {
            List<Policy> policies = policyMap.get(layer);

            // 当前层没有注册任何规则，直接跳过
            if (policies == null || policies.isEmpty()) {
                continue;
            }

            for (Policy policy : policies) {
                PolicyResult result = policy.evaluate(subject, resource, action, environment);

                if (result == PolicyResult.FORCE_ALLOW) {
                    // 安全策略强制放行，跳过所有后续层（临时授权场景）
                    return DecisionResult.forceAllow(policy.getName());
                }

                if (result == PolicyResult.DENY) {
                    // 当前规则拒绝，记录触发规则名，立即终止
                    return DecisionResult.deny(policy.getName(), buildDenyReason(layer, policy));
                }
            }
        }

        // 所有层全部通过，默认允许
        return DecisionResult.allow();
    }

    private String buildDenyReason(PolicyLayer layer, Policy policy) {
        switch (layer) {
            case SECURITY: return "安全策略拒绝：" + policy.getName();
            case PROJECT:  return "项目策略拒绝：" + policy.getName();
            case ROLE:     return "角色属性拒绝：" + policy.getName();
            default:       return "策略拒绝：" + policy.getName();
        }
    }
}
