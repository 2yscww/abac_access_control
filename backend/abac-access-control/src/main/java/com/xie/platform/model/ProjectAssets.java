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

    /**
     * 文件路径或Git地址
     * - 文档类资产：本地路径（如 /uploads/projects/1738051200000/doc.pdf）
     * - 代码类资产：Git URL（如 https://github.com/company/repo.git）
     * - 云存储：OSS URL（如 https://oss.aliyun.com/bucket/file.pdf）
     */
    private String filePath;

    /**
     * 文件大小（字节）
     * - 文档类资产：实际文件大小
     * - 代码类资产：NULL
     */
    private Long fileSize;

    /**
     * 资产描述
     */
    private String description;
}
