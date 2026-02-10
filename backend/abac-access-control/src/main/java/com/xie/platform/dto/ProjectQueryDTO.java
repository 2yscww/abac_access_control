package com.xie.platform.dto;

import lombok.Data;

/**
 * 项目查询条件 DTO
 */
@Data
public class ProjectQueryDTO {

    /** 项目名称（模糊查询） */
    private String projectName;

    /** 项目阶段 */
    private Integer projectPhase;

    /** 保密等级 */
    private Integer securityLevel;

    /** 创建人ID */
    private Long createdByEmployeeId;

    /** 分页：页码（从1开始） */
    private Integer pageNum = 1;

    /** 分页：每页大小 */
    private Integer pageSize = 10;
}
