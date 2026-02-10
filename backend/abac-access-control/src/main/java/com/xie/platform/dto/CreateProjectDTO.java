package com.xie.platform.dto;

import lombok.Data;

/**
 * 创建项目 DTO
 */
@Data
public class CreateProjectDTO {

    /** 项目名称 */
    private String projectName;

    /**
     * 项目初始阶段（默认为立项阶段）
     * 1-立项 2-需求设计 3-研发实现 4-测试验证 5-上线交付 6-归档
     */
    private Integer projectPhase;

    /**
     * 项目保密等级
     * 1-公开 2-内部 3-机密 4-高度机密
     */
    private Integer securityLevel;

    /** 项目ID（可选，不传则自动生成） */
    private Long projectId;
}
