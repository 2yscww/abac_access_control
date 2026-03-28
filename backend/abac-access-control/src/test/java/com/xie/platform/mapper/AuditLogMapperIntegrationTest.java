package com.xie.platform.mapper;

import com.xie.platform.dto.AuditLogQueryDTO;
import com.xie.platform.model.AuditLog;
import com.xie.platform.model.enumValue.NetworkZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class AuditLogMapperIntegrationTest {

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM audit_logs");
        jdbcTemplate.update("DELETE FROM projects");
        jdbcTemplate.update("DELETE FROM employees");

        jdbcTemplate.update("INSERT INTO employees (employee_id) VALUES (?)", 7L);
        jdbcTemplate.update("INSERT INTO projects (project_id) VALUES (?)", 11L);
    }

    @Test
    void insertAndSelect_shouldPersistNetworkZoneEnum() {
        LocalDateTime requestTime = LocalDateTime.of(2026, 3, 27, 19, 0, 0);

        AuditLog auditLog = new AuditLog();
        auditLog.setEmployeeId(7L);
        auditLog.setResourceType("PROJECT");
        auditLog.setResourceId(11L);
        auditLog.setProjectId(11L);
        auditLog.setAction("READ");
        auditLog.setDecision("ALLOW");
        auditLog.setTriggerPolicy("BusinessPolicy");
        auditLog.setRequestIp("10.10.10.8");
        auditLog.setNetworkZone(NetworkZone.INTERNAL);
        auditLog.setRequestUri("/api/projects/11");
        auditLog.setRequestTime(requestTime);
        auditLog.setDetailJson("{\"demo\":true}");

        auditLogMapper.insert(auditLog);

        assertNotNull(auditLog.getLogId());
        assertEquals(
                "INTERNAL",
                jdbcTemplate.queryForObject(
                        "SELECT network_zone FROM audit_logs WHERE log_id = ?",
                        String.class,
                        auditLog.getLogId()
                )
        );

        AuditLogQueryDTO query = new AuditLogQueryDTO();
        query.setEmployeeId(7L);
        query.setPageNum(1);
        query.setPageSize(10);
        query.setOffset(0);

        List<AuditLog> logs = auditLogMapper.selectByCondition(query);

        assertEquals(1, logs.size());
        assertEquals(NetworkZone.INTERNAL, logs.get(0).getNetworkZone());
        assertEquals("/api/projects/11", logs.get(0).getRequestUri());
        assertEquals(1, auditLogMapper.countByCondition(query));
    }

    @Test
    void insert_shouldAllowNullEmployeeIdForAnonymousSecurityEvents() {
        AuditLog auditLog = new AuditLog();
        auditLog.setResourceType("AUTH");
        auditLog.setAction("LOGIN");
        auditLog.setDecision("DENY");
        auditLog.setTriggerPolicy("SECURITY_EVENT");
        auditLog.setDenyReason("员工不存在");
        auditLog.setDetailJson("{\"employeeCode\":\"ghost\"}");

        auditLogMapper.insert(auditLog);

        assertNotNull(auditLog.getLogId());

        AuditLogQueryDTO query = new AuditLogQueryDTO();
        query.setResourceType("AUTH");
        query.setPageNum(1);
        query.setPageSize(10);
        query.setOffset(0);

        List<AuditLog> logs = auditLogMapper.selectByCondition(query);
        assertEquals(1, logs.size());
        assertEquals("DENY", logs.get(0).getDecision());
        assertEquals("SECURITY_EVENT", logs.get(0).getTriggerPolicy());
    }
}
