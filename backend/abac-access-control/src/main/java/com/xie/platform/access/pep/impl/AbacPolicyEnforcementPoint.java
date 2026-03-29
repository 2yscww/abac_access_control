package com.xie.platform.access.pep.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.environment.Environment;
import com.xie.platform.access.pdp.DecisionResult;
import com.xie.platform.access.pdp.PolicyDecisionPoint;
import com.xie.platform.access.pep.AccessDeniedException;
import com.xie.platform.access.pep.PolicyEnforcementPoint;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.access.resource.ResourceType;
import com.xie.platform.access.subject.Subject;
import com.xie.platform.exception.BizException;
import com.xie.platform.mapper.DepartmentMapper;
import com.xie.platform.mapper.EmployeesMapper;
import com.xie.platform.mapper.ProjectAssetsMapper;
import com.xie.platform.mapper.ProjectMapper;
import com.xie.platform.model.Department;
import com.xie.platform.model.Employees;
import com.xie.platform.model.ProjectAssets;
import com.xie.platform.model.Projects;
import com.xie.platform.model.enumValue.NetworkZone;
import com.xie.platform.service.AuditLogService;
import com.xie.platform.utils.NetworkContextUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Component
public class AbacPolicyEnforcementPoint implements PolicyEnforcementPoint {

    private enum AuditLogMode {
        NONE,
        DENY_ONLY,
        ALL
    }

    @Autowired
    private PolicyDecisionPoint pdp;

    @Autowired
    private EmployeesMapper employeesMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ProjectAssetsMapper projectAssetsMapper;

    @Autowired
    private AuditLogService auditLogService;

    @Override
    public DecisionResult decideAccess(Long employeeId, Resource resource, Action action) {
        // decide* is used by list filtering paths, so only denied results are persisted.
        return evaluateAccess(employeeId, resource, action, AuditLogMode.DENY_ONLY);
    }

    @Override
    public void checkAccess(Long employeeId, Resource resource, Action action) {
        // check* represents an explicit operation request and should leave a full audit trail.
        DecisionResult result = evaluateAccess(employeeId, resource, action, AuditLogMode.ALL);
        throwIfDenied(result);
    }

    @Override
    public DecisionResult decideProjectAccess(Long employeeId, Long projectId, Action action) {
        Resource resource = buildResourceForProject(projectId);
        return decideAccess(employeeId, resource, action);
    }

    @Override
    public DecisionResult checkProjectAccess(Long employeeId, Long projectId, Action action) {
        Resource resource = buildResourceForProject(projectId);
        DecisionResult result = evaluateAccess(employeeId, resource, action, AuditLogMode.ALL);
        throwIfDenied(result);
        return result;
    }

    @Override
    public DecisionResult decideAssetAccess(Long employeeId, Long assetId, Action action) {
        Resource resource = buildResourceForAsset(assetId);
        return decideAccess(employeeId, resource, action);
    }

    @Override
    public DecisionResult checkAssetAccess(Long employeeId, Long assetId, Action action) {
        Resource resource = buildResourceForAsset(assetId);
        DecisionResult result = evaluateAccess(employeeId, resource, action, AuditLogMode.ALL);
        throwIfDenied(result);
        return result;
    }

    private DecisionResult evaluateAccess(Long employeeId, Resource resource, Action action, AuditLogMode auditLogMode) {
        Subject subject = buildSubject(employeeId);
        Environment environment = buildEnvironment();
        DecisionResult result = pdp.evaluate(subject, resource, action, environment);

        if (shouldWriteAuditLog(result, auditLogMode)) {
            auditLogService.recordDecision(employeeId, resource, action, environment, result);
        }
        return result;
    }

    private boolean shouldWriteAuditLog(DecisionResult result, AuditLogMode auditLogMode) {
        // decide* is used for list filtering, so only denied results are recorded there.
        return switch (auditLogMode) {
            case NONE -> false;
            case DENY_ONLY -> !result.isAllowed();
            case ALL -> true;
        };
    }

    private void throwIfDenied(DecisionResult result) {
        if (!result.isAllowed()) {
            throw new AccessDeniedException(result.getTriggerPolicy(), result.getReason());
        }
    }

    private Subject buildSubject(Long employeeId) {
        Employees employee = employeesMapper.selectByEmployeeId(employeeId);
        if (employee == null) {
            throw new BizException("员工不存在：" + employeeId);
        }

        Department department = departmentMapper.selectById(employee.getDeptId());
        if (department == null) {
            throw new BizException("部门不存在：" + employee.getDeptId());
        }

        return new Subject(
                employee.getEmployeeId(),
                employee.getDeptId(),
                department.getDeptType(),
                employee.getBranchId(),
                employee.getLevel(),
                employee.getIsContractor()
        );
    }

    private Resource buildResourceForProject(Long projectId) {
        Projects project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BizException("项目不存在：" + projectId);
        }

        return Resource.builder()
                .type(ResourceType.PROJECT)
                .resourceId(project.getProjectId())
                .projectId(project.getProjectId())
                .projectPhase(project.getProjectPhase())
                .securityLevel(project.getSecurityLevel())
                .creatorId(project.getCreatedByEmployeeId())
                .ownerId(project.getOwnerId())
                .deptId(null)
                .build();
    }

    private Resource buildResourceForAsset(Long assetId) {
        ProjectAssets asset = projectAssetsMapper.selectById(assetId);
        if (asset == null) {
            throw new BizException("资产不存在：" + assetId);
        }

        Projects project = projectMapper.selectById(asset.getProjectId());
        if (project == null) {
            throw new BizException("资产所属项目不存在：" + asset.getProjectId());
        }

        return Resource.builder()
                .type(ResourceType.ASSET)
                .resourceId(asset.getAssetId())
                .projectId(asset.getProjectId())
                .projectPhase(project.getProjectPhase())
                .assetsStage(asset.getAssetsStage())
                .securityLevel(asset.getSecurityLevel())
                .assetType(asset.getAssetsType())
                .creatorId(asset.getCreatedByEmployeeId())
                .ownerId(project.getOwnerId())
                .deptId(null)
                .build();
    }

    private Environment buildEnvironment() {
        LocalDateTime requestTime = LocalDateTime.now();
        String ipAddress = "unknown";
        NetworkZone networkZone = NetworkZone.UNKNOWN;

        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                ipAddress = NetworkContextUtil.extractClientIp(request);
                networkZone = NetworkContextUtil.resolveNetworkZone(ipAddress);
                return Environment.builder()
                        .requestTime(requestTime)
                        .ipAddress(ipAddress)
                        .networkZone(networkZone)
                        .requestUri(request.getRequestURI())
                        .build();
            }
        } catch (Exception ignored) {
        }

        return Environment.builder()
                .requestTime(requestTime)
                .ipAddress(ipAddress)
                .networkZone(networkZone)
                .requestUri(null)
                .build();
    }
}
