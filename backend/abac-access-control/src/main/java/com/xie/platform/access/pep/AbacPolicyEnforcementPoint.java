package com.xie.platform.access.pep;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.environment.Environment;
import com.xie.platform.access.pdp.DecisionResult;
import com.xie.platform.access.pdp.PolicyDecisionPoint;
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
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Component
public class AbacPolicyEnforcementPoint implements PolicyEnforcementPoint {

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

    @Override
    public DecisionResult decideAccess(Long employeeId, Resource resource, Action action) {
        Subject subject = buildSubject(employeeId);
        Environment environment = buildEnvironment();
        return pdp.evaluate(subject, resource, action, environment);
    }

    @Override
    public void checkAccess(Long employeeId, Resource resource, Action action) {
        DecisionResult result = decideAccess(employeeId, resource, action);
        throwIfDenied(result);
    }

    @Override
    public DecisionResult decideProjectAccess(Long employeeId, Long projectId, Action action) {
        Resource resource = buildResourceForProject(projectId);
        return decideAccess(employeeId, resource, action);
    }

    @Override
    public DecisionResult checkProjectAccess(Long employeeId, Long projectId, Action action) {
        DecisionResult result = decideProjectAccess(employeeId, projectId, action);
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
        DecisionResult result = decideAssetAccess(employeeId, assetId, action);
        throwIfDenied(result);
        return result;
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
                .projectPhase(project.getProjectPhase())
                .securityLevel(project.getSecurityLevel())
                .creatorId(project.getCreatedByEmployeeId())
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
                .projectPhase(project.getProjectPhase())
                .securityLevel(asset.getSecurityLevel())
                .creatorId(asset.getCreatedByEmployeeId())
                .deptId(null)
                .build();
    }

    private Environment buildEnvironment() {
        LocalDateTime requestTime = LocalDateTime.now();
        String ipAddress = "unknown";

        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                ipAddress = getClientIpAddress(request);
            }
        } catch (Exception ignored) {
        }

        return Environment.builder()
                .requestTime(requestTime)
                .ipAddress(ipAddress)
                .build();
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}
