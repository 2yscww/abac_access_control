package com.xie.platform.access.pep;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.pdp.DecisionResult;

/**
 * 策略执行点接口（PEP）
 *
 * PEP 是 ABAC 的"门卫"，负责：
 * 1. 接收业务层的访问请求
 * 2. 构建 ABAC 四要素（Subject, Resource, Action, Environment）
 * 3. 调用 PDP 做决策
 * 4. 根据决策结果放行或抛出异常
 */
public interface PolicyEnforcementPoint {

    /**
     * 检查员工是否有权对项目执行指定操作
     *
     * @param employeeId 员工ID
     * @param projectId 项目ID
     * @param action 操作类型
     * @return 决策结果
     * @throws AccessDeniedException 如果访问被拒绝
     */
    DecisionResult checkProjectAccess(Long employeeId, Long projectId, Action action);

    /**
     * 检查员工是否有权对资产执行指定操作
     *
     * @param employeeId 员工ID
     * @param assetId 资产ID
     * @param action 操作类型
     * @return 决策结果
     * @throws AccessDeniedException 如果访问被拒绝
     */
    DecisionResult checkAssetAccess(Long employeeId, Long assetId, Action action);
}
