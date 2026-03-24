package com.xie.platform.service.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.dto.AssignDepartmentManagerDTO;
import com.xie.platform.dto.DepartmentManagerHandoverTodoDTO;
import com.xie.platform.dto.EmployeeOptionDTO;
import com.xie.platform.exception.BizException;
import com.xie.platform.mapper.DepartmentMapper;
import com.xie.platform.mapper.EmployeesMapper;
import com.xie.platform.mapper.ProjectMapper;
import com.xie.platform.model.Department;
import com.xie.platform.model.Employees;
import com.xie.platform.model.Projects;
import com.xie.platform.model.enumValue.DeptType;
import com.xie.platform.model.enumValue.EmployeeStatus;
import com.xie.platform.model.enumValue.ProjectPhase;
import com.xie.platform.service.AuditLogService;
import com.xie.platform.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private EmployeesMapper employeesMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private AuditLogService auditLogService;

    @Override
    @Transactional
    public void assignDepartmentManager(AssignDepartmentManagerDTO dto, Long operatorEmployeeId) {
        ensureManagementOperator(operatorEmployeeId);

        if (dto == null || dto.getDeptId() == null) {
            throw new BizException("部门ID不能为空");
        }
        if (dto.getNewManagerEmployeeId() == null) {
            throw new BizException("新负责人ID不能为空");
        }

        Department department = departmentMapper.selectById(dto.getDeptId());
        if (department == null) {
            throw new BizException("部门不存在");
        }

        Employees newManager = employeesMapper.selectByEmployeeId(dto.getNewManagerEmployeeId());
        if (newManager == null) {
            throw new BizException("新负责人不存在");
        }
        if (newManager.getStatus() != EmployeeStatus.ACTIVE) {
            throw new BizException("新负责人状态不可用");
        }
        if (!department.getDeptId().equals(newManager.getDeptId())) {
            throw new BizException("新负责人必须属于同一部门");
        }

        Employees oldManager = department.getManagerId() == null
                ? null
                : employeesMapper.selectByEmployeeId(department.getManagerId());

        List<Integer> phaseCodes = resolveResponsiblePhaseCodes(department.getDeptType());
        List<Projects> affectedProjects = phaseCodes.isEmpty()
                ? List.of()
                : projectMapper.selectByPhaseCodes(phaseCodes);

        departmentMapper.updateManagerId(department.getDeptId(), newManager.getEmployeeId());
        if (!phaseCodes.isEmpty()) {
            projectMapper.updateOwnerByPhaseCodes(phaseCodes, newManager.getEmployeeId());
        }

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("approverId", operatorEmployeeId);
        detail.put("deptId", department.getDeptId());
        detail.put("deptName", department.getDeptName());
        detail.put("deptType", department.getDeptType() != null ? department.getDeptType().name() : null);
        detail.put("oldManagerId", department.getManagerId());
        detail.put("oldManagerCode", oldManager != null ? oldManager.getEmployeeCode() : null);
        detail.put("oldManagerName", oldManager != null ? oldManager.getEmployeeName() : null);
        detail.put("newManagerId", newManager.getEmployeeId());
        detail.put("newManagerCode", newManager.getEmployeeCode());
        detail.put("newManagerName", newManager.getEmployeeName());
        detail.put("ownerSyncPhaseCodes", phaseCodes);
        detail.put("affectedProjectIds", affectedProjects.stream().map(Projects::getProjectId).toList());
        detail.put("affectedProjectCount", affectedProjects.size());

        auditLogService.recordBusinessEvent(
                operatorEmployeeId,
                "DEPARTMENT",
                department.getDeptId(),
                Action.ASSIGN_DEPARTMENT_MANAGER,
                detail
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentManagerHandoverTodoDTO> queryManagerHandoverTodos(Long operatorEmployeeId) {
        ensureManagementOperator(operatorEmployeeId);

        List<DepartmentManagerHandoverTodoDTO> todos = departmentMapper.selectWithInactiveManager();
        Map<DeptType, List<Projects>> affectedProjectCache = new EnumMap<>(DeptType.class);

        for (DepartmentManagerHandoverTodoDTO todo : todos) {
            List<Integer> phaseCodes = resolveResponsiblePhaseCodes(todo.getDeptType());
            List<Projects> affectedProjects;
            if (phaseCodes.isEmpty()) {
                affectedProjects = List.of();
            } else {
                affectedProjects = affectedProjectCache.computeIfAbsent(
                        todo.getDeptType(),
                        ignored -> projectMapper.selectByPhaseCodes(phaseCodes)
                );
            }
            todo.setAffectedProjects(List.copyOf(affectedProjects));
            todo.setAffectedProjectCount(affectedProjects.size());
        }

        return todos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeOptionDTO> queryManagerCandidates(Long deptId, Long operatorEmployeeId) {
        ensureManagementOperator(operatorEmployeeId);

        if (deptId == null) {
            throw new BizException("部门ID不能为空");
        }

        Department department = departmentMapper.selectById(deptId);
        if (department == null) {
            throw new BizException("部门不存在");
        }

        return employeesMapper.selectActiveOptionsByDeptId(deptId);
    }

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
            throw new BizException("仅管理层允许执行该操作");
        }
    }

    private List<Integer> resolveResponsiblePhaseCodes(DeptType deptType) {
        if (deptType == null) {
            return List.of();
        }
        return switch (deptType) {
            case MANAGEMENT -> List.of(ProjectPhase.INIT.getCode(), ProjectPhase.ARCHIVED.getCode());
            case PRODUCT -> List.of(ProjectPhase.REQUIREMENT.getCode());
            case RD -> List.of(ProjectPhase.DEVELOPMENT.getCode());
            case QA -> List.of(ProjectPhase.TEST.getCode());
            case OPS -> List.of(ProjectPhase.RELEASE.getCode());
            case HR -> List.of();
        };
    }
}
