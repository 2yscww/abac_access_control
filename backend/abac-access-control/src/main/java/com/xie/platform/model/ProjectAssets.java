package com.xie.platform.model;

import java.time.LocalDateTime;

import com.xie.platform.model.enumValue.AssetType;
import com.xie.platform.model.enumValue.ProjectPhase;
import com.xie.platform.model.enumValue.SecurityLevel;

import lombok.Data;

@Data
public class ProjectAssets {
    /** 资源唯一标识 */
    private Long assetId;

    /** 所属项目ID */
    private Long projectId;

    /** 资源名称 */
    private String assetName;

    /**
     * 资源类型
     * 例如：需求文档 / 设计文档 / 源代码 / 测试报告 / 部署脚本 / 运维文档
     */
    private AssetType assetsType;

    /**
     * 资源产生阶段（历史快照，不随项目阶段变化）
     * 例如：立项 / 需求设计 / 研发实现 / 测试验证 / 上线交付 / 归档
     */
    private ProjectPhase assetsStage;

    /**
     * 资源密级
     * 例如：公开 / 内部 / 机密 / 高度机密
     */
    private SecurityLevel securityLevel;

    /** 创建人（员工ID） */
    private Long createdByEmployeeId;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
