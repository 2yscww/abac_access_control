package com.xie.platform.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xie.platform.access.action.Action;
import com.xie.platform.access.environment.Environment;
import com.xie.platform.access.pdp.DecisionResult;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.access.resource.ResourceType;
import com.xie.platform.dto.AuditLogQueryDTO;
import com.xie.platform.exception.BizException;
import com.xie.platform.mapper.AuditLogMapper;
import com.xie.platform.mapper.DepartmentMapper;
import com.xie.platform.mapper.EmployeesMapper;
import com.xie.platform.model.AuditLog;
import com.xie.platform.model.Department;
import com.xie.platform.model.Employees;
import com.xie.platform.model.enumValue.DeptType;
import com.xie.platform.model.enumValue.EmployeeStatus;
import com.xie.platform.model.enumValue.NetworkZone;
import com.xie.platform.model.enumValue.ProjectPhase;
import com.xie.platform.model.enumValue.SecurityLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private AuditLogMapper auditLogMapper;

    @Mock
    private EmployeesMapper employeesMapper;

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    @Test
    void recordDecision_shouldPersistAuditSnapshot() {
        LocalDateTime requestTime = LocalDateTime.of(2026, 3, 18, 20, 0, 0);
        Resource resource = Resource.builder()
                .type(ResourceType.ASSET)
                .resourceId(55L)
                .projectId(11L)
                .projectPhase(ProjectPhase.DEVELOPMENT)
                .assetsStage(ProjectPhase.DEVELOPMENT)
                .securityLevel(SecurityLevel.INTERNAL)
                .creatorId(7L)
                .build();
        Environment environment = Environment.builder()
                .requestTime(requestTime)
                .ipAddress("10.10.10.8")
                .networkZone(NetworkZone.INTERNAL)
                .requestUri("/api/asset/55")
                .build();
        DecisionResult result = DecisionResult.deny("SecurityLevelPolicy", "安全策略拒绝：SecurityLevelPolicy");

        auditLogService.recordDecision(7L, resource, Action.READ, environment, result);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());

        AuditLog auditLog = captor.getValue();
        assertEquals(7L, auditLog.getEmployeeId());
        assertEquals("ASSET", auditLog.getResourceType());
        assertEquals(55L, auditLog.getResourceId());
        assertEquals(11L, auditLog.getProjectId());
        assertEquals("READ", auditLog.getAction());
        assertEquals("DENY", auditLog.getDecision());
        assertEquals("SecurityLevelPolicy", auditLog.getTriggerPolicy());
        assertEquals("安全策略拒绝：SecurityLevelPolicy", auditLog.getDenyReason());
        assertEquals(ProjectPhase.DEVELOPMENT.getCode(), auditLog.getProjectPhase());
        assertEquals(ProjectPhase.DEVELOPMENT.getCode(), auditLog.getAssetsStage());
        assertEquals(SecurityLevel.INTERNAL.getLevel(), auditLog.getSecurityLevel());
        assertEquals("10.10.10.8", auditLog.getRequestIp());
        assertEquals(NetworkZone.INTERNAL, auditLog.getNetworkZone());
        assertEquals("/api/asset/55", auditLog.getRequestUri());
        assertEquals(requestTime, auditLog.getRequestTime());
        assertNull(auditLog.getDetailJson());
    }

    @Test
    void recordBusinessEvent_shouldPersistStructuredDetail() throws Exception {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("deptId", 3L);
        detail.put("newManagerId", 9L);

        when(objectMapper.writeValueAsString(detail))
                .thenReturn("{\"deptId\":3,\"newManagerId\":9}");

        auditLogService.recordBusinessEvent(
                7L,
                "DEPARTMENT",
                3L,
                Action.ASSIGN_DEPARTMENT_MANAGER,
                detail
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());

        AuditLog auditLog = captor.getValue();
        assertEquals(7L, auditLog.getEmployeeId());
        assertEquals("DEPARTMENT", auditLog.getResourceType());
        assertEquals(3L, auditLog.getResourceId());
        assertEquals("ASSIGN_DEPARTMENT_MANAGER", auditLog.getAction());
        assertEquals("ALLOW", auditLog.getDecision());
        assertEquals("BUSINESS", auditLog.getTriggerPolicy());
        assertEquals("{\"deptId\":3,\"newManagerId\":9}", auditLog.getDetailJson());
        assertEquals(NetworkZone.UNKNOWN, auditLog.getNetworkZone());
        assertNotNull(auditLog.getRequestTime());
        assertNull(auditLog.getProjectId());
    }

    @Test
    void recordBusinessEvent_shouldResolveForwardedIpAndNetworkZone() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.1");
        request.setRequestURI("/api/department/assign-manager");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Map<String, Object> detail = Map.of("deptId", 3L);
        when(objectMapper.writeValueAsString(detail)).thenReturn("{\"deptId\":3}");

        try {
            auditLogService.recordBusinessEvent(
                    7L,
                    "DEPARTMENT",
                    3L,
                    Action.ASSIGN_DEPARTMENT_MANAGER,
                    detail
            );
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());

        AuditLog auditLog = captor.getValue();
        assertEquals("203.0.113.9", auditLog.getRequestIp());
        assertEquals(NetworkZone.PUBLIC, auditLog.getNetworkZone());
        assertEquals("/api/department/assign-manager", auditLog.getRequestUri());
    }

    @Test
    void recordSecurityEvent_shouldAllowAnonymousFailureAudit() throws Exception {
        Map<String, Object> detail = Map.of("employeeCode", "ghost");
        when(objectMapper.writeValueAsString(detail)).thenReturn("{\"employeeCode\":\"ghost\"}");

        auditLogService.recordSecurityEvent(
                null,
                "AUTH",
                null,
                Action.LOGIN,
                "员工不存在",
                detail
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());

        AuditLog auditLog = captor.getValue();
        assertNull(auditLog.getEmployeeId());
        assertEquals("AUTH", auditLog.getResourceType());
        assertEquals("LOGIN", auditLog.getAction());
        assertEquals("DENY", auditLog.getDecision());
        assertEquals("SECURITY_EVENT", auditLog.getTriggerPolicy());
        assertEquals("员工不存在", auditLog.getDenyReason());
        assertEquals("{\"employeeCode\":\"ghost\"}", auditLog.getDetailJson());
    }

    @Test
    void queryAuditLogs_shouldRejectNonManagementOperator() {
        Employees operator = buildOperator(9L, 2L, EmployeeStatus.ACTIVE);
        Department department = new Department();
        department.setDeptId(2L);
        department.setDeptType(DeptType.RD);

        when(employeesMapper.selectByEmployeeId(9L)).thenReturn(operator);
        when(departmentMapper.selectById(2L)).thenReturn(department);

        BizException exception = assertThrows(
                BizException.class,
                () -> auditLogService.queryAuditLogs(new AuditLogQueryDTO(), 9L)
        );

        assertEquals("仅管理层允许查看审计日志", exception.getMessage());
        verifyNoInteractions(auditLogMapper);
    }

    @Test
    void queryAuditLogs_shouldReturnPagedResultForManagementOperator() {
        Employees operator = buildOperator(9L, 3L, EmployeeStatus.ACTIVE);
        Department department = new Department();
        department.setDeptId(3L);
        department.setDeptType(DeptType.MANAGEMENT);

        AuditLog log = new AuditLog();
        log.setLogId(100L);
        log.setDecision("ALLOW");

        AuditLogQueryDTO query = new AuditLogQueryDTO();
        query.setPageNum(2);
        query.setPageSize(5);
        query.setDecision("ALLOW");

        when(employeesMapper.selectByEmployeeId(9L)).thenReturn(operator);
        when(departmentMapper.selectById(3L)).thenReturn(department);
        when(auditLogMapper.selectByCondition(any(AuditLogQueryDTO.class))).thenReturn(List.of(log));
        when(auditLogMapper.countByCondition(any(AuditLogQueryDTO.class))).thenReturn(12);

        Map<String, Object> result = auditLogService.queryAuditLogs(query, 9L);

        ArgumentCaptor<AuditLogQueryDTO> captor = ArgumentCaptor.forClass(AuditLogQueryDTO.class);
        verify(auditLogMapper).selectByCondition(captor.capture());

        AuditLogQueryDTO normalized = captor.getValue();
        assertEquals(2, normalized.getPageNum());
        assertEquals(5, normalized.getPageSize());
        assertEquals(5, normalized.getOffset());
        assertEquals("ALLOW", normalized.getDecision());

        assertEquals(12, result.get("total"));
        assertEquals(2, result.get("pageNum"));
        assertEquals(5, result.get("pageSize"));
        assertEquals(List.of(log), result.get("list"));
        assertNull(normalized.getStartTime());
    }

    private Employees buildOperator(Long employeeId, Long deptId, EmployeeStatus status) {
        Employees operator = new Employees();
        operator.setEmployeeId(employeeId);
        operator.setDeptId(deptId);
        operator.setStatus(status);
        return operator;
    }
}
