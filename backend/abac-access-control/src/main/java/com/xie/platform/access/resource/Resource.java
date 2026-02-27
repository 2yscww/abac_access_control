package com.xie.platform.access.resource;

import com.xie.platform.model.enumValue.ProjectPhase;
import com.xie.platform.model.enumValue.SecurityLevel;
import lombok.Builder;
import lombok.Data;

/**
 * ABAC 资源模型
 * 封装被访问资源的关键属性，供 PDP 做决策时使用。
 * 不直接使用 Projects / ProjectAssets 实体，是为了让 PDP 与具体业务模型解耦。
 */
@Data
@Builder
public class Resource {

    /**
     * 资源类型
     * 用于区分当前被访问的是项目还是资产
     */
    private ResourceType type;

    /**
     * 资源所属项目的当前阶段
     * 这是阶段访问控制矩阵的核心判断依据
     */
    private ProjectPhase projectPhase;

    /**
     * 资源的保密等级
     * 用于判断主体的职级是否有权访问该密级的资源
     */
    private SecurityLevel securityLevel;

    /**
     * 资源创建人的 employeeId
     * 用于判断"是否是自己创建的资源"这类规则
     */
    private Long creatorId;

    /**
     * 资源所属部门 ID
     * 用于判断跨部门访问场景
     */
    private Long deptId;
}
