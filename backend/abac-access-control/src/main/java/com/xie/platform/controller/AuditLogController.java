package com.xie.platform.controller;

import com.xie.platform.common.Response;
import com.xie.platform.dto.AuditLogQueryDTO;
import com.xie.platform.service.AuditLogService;
import com.xie.platform.utils.CurrentUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/audit")
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/list")
    public Response<Map<String, Object>> queryAuditLogs(AuditLogQueryDTO query) {
        Long employeeId = CurrentUserContext.getRequiredEmployeeId();
        Map<String, Object> result = auditLogService.queryAuditLogs(query, employeeId);
        return Response.Success(result, null);
    }
}
