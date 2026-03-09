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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * ABAC 策略执行点实现
 *
 * 职责：
 * 1. 从数据库获取主体、资源的完整属性信息（PIP功能）
 * 2. 构建 ABAC 四要素（Subject, Resource, Action, Environment）
 * 3. 调用 PDP 做决策
 * 4. 根据决策结果放行或抛出 AccessDeniedException
 */
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
    public DecisionResult checkProjectAccess(Long employeeId, Long projectId, Action action) {
        // 1. 构建 Subject（主体）
        Subject subject = buildSubject(employeeId);

        // 2. 构建 Resource（资源）
        Resource resource = buildResourceForProject(projectId);

        // 3. 构建 Environment（环境）
        Environment environment = buildEnvironment();

        // 4. 调用 PDP 做决策
        DecisionResult result = pdp.evaluate(subject, resource, action, environment);

        // 5. 如果拒绝，抛出异常
        if (!result.isAllowed()) {
            throw new AccessDeniedException(result.getTriggerPolicy(), result.getReason());
        }

        return result;
    }

    @Override
    public DecisionResult checkAssetAccess(Long employeeId, Long assetId, Action action) {
        // 1. 构建 Subject（主体）
        Subject subject = buildSubject(employeeId);

        // 2. 构建 Resource（资源）
        Resource resource = buildResourceForAsset(assetId);

        // 3. 构建 Environment（环境）
        Environment environment = buildEnvironment();

        // 4. 调用 PDP 做决策
        DecisionResult result = pdp.evaluate(subject, resource, action, environment);

        // 5. 如果拒绝，抛出异常
        if (!result.isAllowed()) {
            throw new AccessDeniedException(result.getTriggerPolicy(), result.getReason());
        }

        return result;
    }

    /**
     * 构建 Subject（主体）
     * 从数据库查询员工的完整属性信息
     */
    private Subject buildSubject(Long employeeId) {
        // 查询员工信息
        Employees employee = employeesMapper.selectByEmployeeId(employeeId);
        if (employee == null) {
            throw new BizException("员工不存在：" + employeeId);
        }

        // 查询部门信息（获取部门类型）
        Department department = departmentMapper.selectById(employee.getDeptId());
        if (department == null) {
            throw new BizException("部门不存在：" + employee.getDeptId());
        }

        // 构建 Subject
        return new Subject(
                employee.getEmployeeId(),
                employee.getDeptId(),
                department.getDeptType(),
                employee.getBranchId(),
                employee.getLevel(),
                employee.getIsContractor()
        );
    }

    /**
     * 构建 Resource（资源）- 项目
     */
    private Resource buildResourceForProject(Long projectId) {
        // 查询项目信息
        Projects project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BizException("项目不存在：" + projectId);
        }

        // 构建 Resource
        return Resource.builder()
                .type(ResourceType.PROJECT)
                .projectPhase(project.getProjectPhase())
                .securityLevel(project.getSecurityLevel())
                .creatorId(project.getCreatedByEmployeeId())
                .deptId(null)  // 项目不直接归属部门
                .build();
    }

    /**
     * 构建 Resource（资源）- 资产
     */
    private Resource buildResourceForAsset(Long assetId) {
        // 查询资产信息
        ProjectAssets asset = projectAssetsMapper.selectById(assetId);
        if (asset == null) {
            throw new BizException("资产不存在：" + assetId);
        }

        // 查询资产所属项目（获取项目阶段）
        Projects project = projectMapper.selectById(asset.getProjectId());
        if (project == null) {
            throw new BizException("资产所属项目不存在：" + asset.getProjectId());
        }

        // 构建 Resource
        return Resource.builder()
                .type(ResourceType.ASSET)
                .projectPhase(project.getProjectPhase())  // 使用项目当前阶段
                .securityLevel(asset.getSecurityLevel())
                .creatorId(asset.getCreatedByEmployeeId())
                .deptId(null)  // 资产不直接归属部门
                .build();
    }

    /**
     * 构建 Environment（环境上下文）
     * 从当前 HTTP 请求中提取环境信息
     */
    private Environment buildEnvironment() {
        LocalDateTime requestTime = LocalDateTime.now();
        String ipAddress = "unknown";

        try {
            // 尝试从 Spring 上下文获取当前请求
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                ipAddress = getClientIpAddress(request);
            }
        } catch (Exception e) {
            // 非 Web 请求场景（如定时任务、测试），使用默认值
        }

        return Environment.builder()
                .requestTime(requestTime)
                .ipAddress(ipAddress)
                .build();
    }

    /**
     * 获取客户端真实 IP 地址
     * 考虑代理和负载均衡的情况
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // X-Forwarded-For 可能包含多个 IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}
