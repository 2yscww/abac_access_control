package com.xie.platform.model;

import com.xie.platform.model.enumValue.ProjectPhase;
import com.xie.platform.model.enumValue.SecurityLevel;

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
     * 对应 ProjectPhase 枚举
     * 作为 ABAC 决策中的 Resource Attribute
     */
    private ProjectPhase projectPhase;

    /**
     * 项目保密等级
     * 对应 SecurityLevel 枚举
     * 控制项目"可见性"
     */
    private SecurityLevel securityLevel;

    /** 项目创建人（员工ID） */
    private Long createdByEmployeeId;

    /** 当前阶段实际负责人（员工ID） */
    private Long ownerId;

    /** 项目创建时间 */
    private LocalDateTime createdAt;

    /** 项目更新时间 */
    private LocalDateTime updatedAt;
}
