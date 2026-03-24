package com.xie.platform.service.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.environment.Environment;
import com.xie.platform.access.pdp.DecisionResult;
import com.xie.platform.access.resource.Resource;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xie.platform.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Autowired
    private EmployeesMapper employeesMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDecision(
            Long employeeId,
            Resource resource,
            Action action,
            Environment environment,
            DecisionResult decisionResult) {

        AuditLog auditLog = new AuditLog();
        auditLog.setEmployeeId(employeeId);
        auditLog.setResourceType(resource.getType() != null ? resource.getType().name() : null);
        auditLog.setResourceId(resource.getResourceId());
        auditLog.setProjectId(resource.getProjectId());
        auditLog.setAction(action.name());
        auditLog.setDecision(decisionResult.isAllowed() ? "ALLOW" : "DENY");
        auditLog.setTriggerPolicy(decisionResult.getTriggerPolicy());
        auditLog.setDenyReason(decisionResult.isAllowed() ? null : decisionResult.getReason());
        auditLog.setProjectPhase(resource.getProjectPhase() != null ? resource.getProjectPhase().getCode() : null);
        auditLog.setAssetsStage(resource.getAssetsStage() != null ? resource.getAssetsStage().getCode() : null);
        auditLog.setSecurityLevel(resource.getSecurityLevel() != null ? resource.getSecurityLevel().getLevel() : null);
        auditLog.setRequestIp(environment != null ? environment.getIpAddress() : null);
        auditLog.setRequestUri(environment != null ? environment.getRequestUri() : null);

        // Record the authorization decision time rather than the final business commit time.
        auditLog.setRequestTime(environment != null ? environment.getRequestTime() : LocalDateTime.now());
        auditLogMapper.insert(auditLog);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordBusinessEvent(
            Long employeeId,
            String resourceType,
            Long resourceId,
            Action action,
            Map<String, Object> detail) {
        if (employeeId == null) {
            throw new BizException("业务审计缺少操作人");
        }
        if (resourceType == null || resourceType.isBlank()) {
            throw new BizException("业务审计缺少资源类型");
        }
        if (action == null) {
            throw new BizException("业务审计缺少动作类型");
        }

        AuditLog auditLog = new AuditLog();
        auditLog.setEmployeeId(employeeId);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        auditLog.setAction(action.name());
        auditLog.setDecision("ALLOW");
        auditLog.setTriggerPolicy("BUSINESS");
        auditLog.setRequestTime(LocalDateTime.now());
        auditLog.setDetailJson(serializeDetail(detail));

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            auditLog.setRequestIp(request.getRemoteAddr());
            auditLog.setRequestUri(request.getRequestURI());
        }

        auditLogMapper.insert(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> queryAuditLogs(AuditLogQueryDTO query, Long operatorEmployeeId) {
        AuditLogQueryDTO normalizedQuery = normalizeQuery(query);
        ensureManagementOperator(operatorEmployeeId);

        List<AuditLog> logs = auditLogMapper.selectByCondition(normalizedQuery);
        int total = auditLogMapper.countByCondition(normalizedQuery);

        Map<String, Object> result = new HashMap<>();
        result.put("list", logs);
        result.put("total", total);
        result.put("pageNum", normalizedQuery.getPageNum());
        result.put("pageSize", normalizedQuery.getPageSize());
        return result;
    }

    private AuditLogQueryDTO normalizeQuery(AuditLogQueryDTO query) {
        AuditLogQueryDTO source = query != null ? query : new AuditLogQueryDTO();
        int pageNum = source.getPageNum() == null || source.getPageNum() < 1 ? 1 : source.getPageNum();
        int pageSize = source.getPageSize() == null || source.getPageSize() < 1 ? 10 : source.getPageSize();

        if (source.getStartTime() != null
                && source.getEndTime() != null
                && source.getStartTime().isAfter(source.getEndTime())) {
            throw new BizException("审计查询开始时间不能晚于结束时间");
        }

        AuditLogQueryDTO normalized = new AuditLogQueryDTO();
        normalized.setEmployeeId(source.getEmployeeId());
        normalized.setResourceType(source.getResourceType());
        normalized.setResourceId(source.getResourceId());
        normalized.setProjectId(source.getProjectId());
        normalized.setAction(source.getAction());
        normalized.setDecision(source.getDecision());
        normalized.setSecurityLevel(source.getSecurityLevel());
        normalized.setStartTime(source.getStartTime());
        normalized.setEndTime(source.getEndTime());
        normalized.setPageNum(pageNum);
        normalized.setPageSize(pageSize);
        normalized.setOffset((pageNum - 1) * pageSize);
        return normalized;
    }

    /**
     * 审计日志本身属于敏感数据，第一版先只允许管理层查看。
     * 这样既符合你的业务设定，也能避免普通员工反向窥探别人访问痕迹。
     */
    private void ensureManagementOperator(Long operatorEmployeeId) {
        Employees operator = employeesMapper.selectByEmployeeId(operatorEmployeeId);
        if (operator == null) {
            throw new BizException("当前操作人不存在");
        }
        if (operator.getStatus() != EmployeeStatus.ACTIVE) {
            throw new BizException("当前操作人状态不可用");
        }

        Department department = departmentMapper.selectById(operator.getDeptId());
        if (department == null) {
            throw new BizException("当前操作人所属部门不存在");
        }
        if (department.getDeptType() != DeptType.MANAGEMENT) {
            throw new BizException("仅管理层允许查看审计日志");
        }
    }

    private String serializeDetail(Map<String, Object> detail) {
        if (detail == null || detail.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new BizException("业务审计明细序列化失败");
        }
    }
}
