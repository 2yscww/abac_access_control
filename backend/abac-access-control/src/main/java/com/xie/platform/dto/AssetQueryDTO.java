package com.xie.platform.dto;

import lombok.Data;

/**
 * 资产查询条件 DTO
 */
@Data
public class AssetQueryDTO {

    /** 所属项目ID */
    private Long projectId;

    /** 资产名称（模糊查询） */
    private String assetName;

    /** 资产类型 */
    private Integer assetsType;

    /** 资产产生阶段 */
    private Integer assetsStage;

    /** 资产密级 */
    private Integer securityLevel;

    /** 创建人ID */
    private Long createdByEmployeeId;

    /** 分页：页码（从1开始） */
    private Integer pageNum = 1;

    /** 分页：每页大小 */
    private Integer pageSize = 10;
}
