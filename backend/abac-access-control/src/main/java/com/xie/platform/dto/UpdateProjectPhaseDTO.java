package com.xie.platform.dto;

import lombok.Data;

/**
 * 更新项目阶段 DTO
 */
@Data
public class UpdateProjectPhaseDTO {

    /** 项目ID */
    private Long projectId;

    /**
     * 新的项目阶段
     * 1-立项 2-需求设计 3-研发实现 4-测试验证 5-上线交付 6-归档
     */
    private Integer newPhase;
}
