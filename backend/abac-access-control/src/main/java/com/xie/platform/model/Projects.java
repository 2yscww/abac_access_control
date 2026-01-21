package com.xie.platform.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目实体（ABAC 资源模型）
 */

@Data
public class Projects {

    /** 项目唯一标识 */
    private Long projectId;

    /** 项目名称 */
    private String projectName;

    /**
     * 项目阶段
     * 对应 ProjectPhaseEnum.code
     * 作为 ABAC 决策中的 Resource Attribute
     */
    private Integer projectPhase;

    /** 项目创建人（员工ID） */
    private Long createdByEmployeeId;

    /** 项目创建时间 */
    private LocalDateTime createdAt;

    /** 项目更新时间 */
    private LocalDateTime updatedAt;
}

