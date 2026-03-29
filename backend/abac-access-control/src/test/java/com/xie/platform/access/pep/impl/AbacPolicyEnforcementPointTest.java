package com.xie.platform.access.pep.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.environment.Environment;
import com.xie.platform.access.pdp.DecisionResult;
import com.xie.platform.access.pdp.PolicyDecisionPoint;
import com.xie.platform.access.pep.AccessDeniedException;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.mapper.DepartmentMapper;
import com.xie.platform.mapper.EmployeesMapper;
import com.xie.platform.mapper.ProjectAssetsMapper;
import com.xie.platform.mapper.ProjectMapper;
import com.xie.platform.model.Department;
import com.xie.platform.model.Employees;
import com.xie.platform.model.ProjectAssets;
import com.xie.platform.model.enumValue.AssetType;
import com.xie.platform.model.Projects;
import com.xie.platform.model.enumValue.DeptType;
import com.xie.platform.model.enumValue.EmployeeLevel;
import com.xie.platform.model.enumValue.ProjectPhase;
import com.xie.platform.model.enumValue.SecurityLevel;
import com.xie.platform.service.AuditLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbacPolicyEnforcementPointTest {

    @Mock
    private PolicyDecisionPoint pdp;

    @Mock
    private EmployeesMapper employeesMapper;

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectAssetsMapper projectAssetsMapper;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AbacPolicyEnforcementPoint pep;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void checkProjectAccess_shouldRecordAllowDecision() {
        mockCurrentRequest("GET", "/api/project/11", "172.16.10.5");
        mockEmployeeContext(1L, 2L);

        Projects project = new Projects();
        project.setProjectId(11L);
        project.setProjectPhase(ProjectPhase.DEVELOPMENT);
        project.setSecurityLevel(SecurityLevel.INTERNAL);
        project.setCreatedByEmployeeId(6L);

        when(projectMapper.selectById(11L)).thenReturn(project);
        when(pdp.evaluate(any(), any(), eq(Action.READ), any())).thenReturn(DecisionResult.allow());

        DecisionResult result = pep.checkProjectAccess(1L, 11L, Action.READ);

        assertTrue(result.isAllowed());

        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        ArgumentCaptor<Environment> environmentCaptor = ArgumentCaptor.forClass(Environment.class);
        ArgumentCaptor<DecisionResult> decisionCaptor = ArgumentCaptor.forClass(DecisionResult.class);
        verify(auditLogService).recordDecision(eq(1L), resourceCaptor.capture(), eq(Action.READ), environmentCaptor.capture(), decisionCaptor.capture());

        Resource resource = resourceCaptor.getValue();
        assertEquals(11L, resource.getResourceId());
        assertEquals(11L, resource.getProjectId());
        assertEquals(ProjectPhase.DEVELOPMENT, resource.getProjectPhase());

        Environment environment = environmentCaptor.getValue();
        assertEquals("172.16.10.5", environment.getIpAddress());
        assertEquals("/api/project/11", environment.getRequestUri());
        assertTrue(decisionCaptor.getValue().isAllowed());
    }

    @Test
    void checkAssetAccess_shouldRecordDeniedDecisionBeforeThrowing() {
        mockCurrentRequest("GET", "/api/asset/20", "172.16.10.9");
        mockEmployeeContext(1L, 2L);

        ProjectAssets asset = new ProjectAssets();
        asset.setAssetId(20L);
        asset.setProjectId(11L);
        asset.setAssetsType(AssetType.SOURCE_CODE);
        asset.setAssetsStage(ProjectPhase.DEVELOPMENT);
        asset.setSecurityLevel(SecurityLevel.INTERNAL);
        asset.setCreatedByEmployeeId(7L);

        Projects project = new Projects();
        project.setProjectId(11L);
        project.setProjectPhase(ProjectPhase.DEVELOPMENT);
        project.setSecurityLevel(SecurityLevel.INTERNAL);
        project.setCreatedByEmployeeId(6L);

        when(projectAssetsMapper.selectById(20L)).thenReturn(asset);
        when(projectMapper.selectById(11L)).thenReturn(project);
        when(pdp.evaluate(any(), any(), eq(Action.READ), any()))
                .thenReturn(DecisionResult.deny("PhaseAccessPolicy", "denied"));

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> pep.checkAssetAccess(1L, 20L, Action.READ)
        );

        assertEquals("PhaseAccessPolicy", exception.getPolicyName());
        assertEquals("denied", exception.getReason());

        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        ArgumentCaptor<DecisionResult> decisionCaptor = ArgumentCaptor.forClass(DecisionResult.class);
        verify(auditLogService).recordDecision(eq(1L), resourceCaptor.capture(), eq(Action.READ), any(Environment.class), decisionCaptor.capture());

        Resource resource = resourceCaptor.getValue();
        assertEquals(20L, resource.getResourceId());
        assertEquals(11L, resource.getProjectId());
        assertEquals(ProjectPhase.DEVELOPMENT, resource.getAssetsStage());
        assertEquals(AssetType.SOURCE_CODE, resource.getAssetType());

        DecisionResult decision = decisionCaptor.getValue();
        assertFalse(decision.isAllowed());
        assertEquals("PhaseAccessPolicy", decision.getTriggerPolicy());
    }

    @Test
    void decideAssetAccess_shouldNotWriteAuditLogForAllowDecision() {
        mockCurrentRequest("GET", "/api/asset/20", "172.16.10.9");
        mockEmployeeContext(1L, 2L);

        ProjectAssets asset = new ProjectAssets();
        asset.setAssetId(20L);
        asset.setProjectId(11L);
        asset.setAssetsType(AssetType.SOURCE_CODE);
        asset.setAssetsStage(ProjectPhase.DEVELOPMENT);
        asset.setSecurityLevel(SecurityLevel.INTERNAL);
        asset.setCreatedByEmployeeId(7L);

        Projects project = new Projects();
        project.setProjectId(11L);
        project.setProjectPhase(ProjectPhase.DEVELOPMENT);
        project.setSecurityLevel(SecurityLevel.INTERNAL);
        project.setCreatedByEmployeeId(6L);

        when(projectAssetsMapper.selectById(20L)).thenReturn(asset);
        when(projectMapper.selectById(11L)).thenReturn(project);
        when(pdp.evaluate(any(), any(), eq(Action.READ), any())).thenReturn(DecisionResult.allow());

        DecisionResult result = pep.decideAssetAccess(1L, 20L, Action.READ);

        assertTrue(result.isAllowed());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void decideAssetAccess_shouldRecordDeniedDecisionForListFiltering() {
        mockCurrentRequest("GET", "/api/asset/list", "172.16.10.9");
        mockEmployeeContext(1L, 2L);

        ProjectAssets asset = new ProjectAssets();
        asset.setAssetId(20L);
        asset.setProjectId(11L);
        asset.setAssetsType(AssetType.SOURCE_CODE);
        asset.setAssetsStage(ProjectPhase.DEVELOPMENT);
        asset.setSecurityLevel(SecurityLevel.INTERNAL);
        asset.setCreatedByEmployeeId(7L);

        Projects project = new Projects();
        project.setProjectId(11L);
        project.setProjectPhase(ProjectPhase.DEVELOPMENT);
        project.setSecurityLevel(SecurityLevel.INTERNAL);
        project.setCreatedByEmployeeId(6L);

        when(projectAssetsMapper.selectById(20L)).thenReturn(asset);
        when(projectMapper.selectById(11L)).thenReturn(project);
        when(pdp.evaluate(any(), any(), eq(Action.READ), any()))
                .thenReturn(DecisionResult.deny("SecurityLevelPolicy", "denied"));

        DecisionResult result = pep.decideAssetAccess(1L, 20L, Action.READ);

        assertFalse(result.isAllowed());
        verify(auditLogService).recordDecision(eq(1L), any(Resource.class), eq(Action.READ), any(Environment.class), eq(result));
    }

    private void mockCurrentRequest(String method, String uri, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(remoteAddr);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private void mockEmployeeContext(Long employeeId, Long deptId) {
        Employees employee = new Employees();
        employee.setEmployeeId(employeeId);
        employee.setDeptId(deptId);
        employee.setBranchId(3L);
        employee.setLevel(EmployeeLevel.P6);
        employee.setIsContractor(false);

        Department department = new Department();
        department.setDeptId(deptId);
        department.setDeptType(DeptType.RD);

        when(employeesMapper.selectByEmployeeId(employeeId)).thenReturn(employee);
        when(departmentMapper.selectById(deptId)).thenReturn(department);
    }
}
