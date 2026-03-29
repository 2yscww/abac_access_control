package com.xie.platform.access.resource;

import com.xie.platform.model.enumValue.AssetType;
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
     * 当前资源的业务主键
     * - PROJECT: projectId
     * - ASSET: assetId
     * - 创建类操作: 可能为空
     */
    private Long resourceId;

    /**
     * 所属项目 ID
     * 对项目资源来说等于 projectId，对资产资源来说等于资产所属项目。
     */
    private Long projectId;

    /**
     * 资源所属项目的当前阶段
     * 这是阶段访问控制矩阵的核心判断依据
     */
    private ProjectPhase projectPhase;

    /**
     * 资产产生阶段（历史快照）
     * 仅对 ASSET 资源有意义，用于区分“当前项目阶段”和“资产最初产生于哪个阶段”。
     */
    private ProjectPhase assetsStage;

    /**
     * 资源的保密等级
     * 用于判断主体的职级是否有权访问该密级的资源
     */
    private SecurityLevel securityLevel;

    /**
     * 资产类型
     * 仅对 ASSET 资源有意义，用于判断某类资产是否属于当前部门职责范围。
     */
    private AssetType assetType;

    /**
     * 资源创建人的 employeeId
     * 用于判断"是否是自己创建的资源"这类规则
     */
    private Long creatorId;

    /**
     * 当前阶段负责人（员工ID）
     * 用于判断谁拥有阶段推进的拍板权
     */
    private Long ownerId;

    /**
     * 资源所属部门 ID
     * 用于判断跨部门访问场景
     */
    private Long deptId;
}
