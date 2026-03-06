package com.xie.platform.access.policy;

/**
 * 策略优先级层
 * 对应文档中定义的优先级顺序：安全策略 > 项目策略 > 角色属性
 *
 * PDP 按层从高到低依次评估，高优先级层拒绝后不再执行低优先级层。
 */
public enum PolicyLayer {

    /** 安全策略层（最高）：密级限制、外包人员限制、临时授权等 */
    SECURITY,

    /** 项目策略层（次高）：项目阶段访问控制矩阵 */
    PROJECT,

    /** 角色属性层（最低）：基于员工角色的细粒度规则（预留扩展） */
    ROLE
}
