package com.xie.platform.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class AuditLogQueryDTO {

    private Long employeeId;
    private String resourceType;
    private Long resourceId;
    private Long projectId;
    private String action;
    private String decision;
    private Integer securityLevel;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private Integer pageNum = 1;
    private Integer pageSize = 10;

    // Calculated inside the service layer for SQL pagination.
    private Integer offset;
}
