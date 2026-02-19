package com.xie.platform.dto;

import lombok.Data;

/**
 * 创建资产 DTO
 */
@Data
public class CreateAssetDTO {

    /** 所属项目ID */
    private Long projectId;

    /** 资产名称 */
    private String assetName;

    /** 资产类型（1-6） */
    private Integer assetsType;

    /** 资产产生阶段（1-6） */
    private Integer assetsStage;

    /** 资产密级（1-4） */
    private Integer securityLevel;

    /**
     * 文件路径或Git地址
     * - 文档类：本地路径或云存储URL
     * - 代码类：Git仓库地址
     */
    private String filePath;

    /** 文件大小（字节），代码类资产可为空 */
    private Long fileSize;

    /** 资产描述 */
    private String description;
}
