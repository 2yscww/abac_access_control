package com.xie.platform.model;

import com.xie.platform.model.enumValue.NetworkZone;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLog {

    private Long logId;

    /** 发起本次访问的员工，匿名失败事件允许为空 */
    private Long employeeId;

    /** 资源类型快照：PROJECT / ASSET */
    private String resourceType;

    /**
     * 资源 ID 快照。
     * - 项目日志：对应 project_id
     * - 资产日志：对应 asset_id
     * - 创建类操作：可能为空，因为资源尚未真正入库
     */
    private Long resourceId;

    /** 所属项目 ID，便于做项目维度审计统计 */
    private Long projectId;

    private String action;
    private String decision;
    private String triggerPolicy;
    private String denyReason;

    /** 资源属性快照，避免后续资源变更导致历史审计失真 */
    private Integer projectPhase;
    private Integer assetsStage;
    private Integer securityLevel;

    private String requestIp;
    private NetworkZone networkZone;
    private String requestUri;
    private LocalDateTime requestTime;
    private String detailJson;
}
