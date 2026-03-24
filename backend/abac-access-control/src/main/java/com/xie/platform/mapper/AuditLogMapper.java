package com.xie.platform.mapper;

import com.xie.platform.dto.AuditLogQueryDTO;
import com.xie.platform.model.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AuditLogMapper {

    int insert(AuditLog auditLog);

    List<AuditLog> selectByCondition(AuditLogQueryDTO query);

    int countByCondition(AuditLogQueryDTO query);

    int countRecentAllowedActions(
            @Param("employeeId") Long employeeId,
            @Param("action") String action,
            @Param("resourceType") String resourceType,
            @Param("startTime") LocalDateTime startTime
    );
}
