package com.xie.platform.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xie.platform.dto.PolicyConfigDTO;
import com.xie.platform.dto.UpdatePolicyConfigDTO;
import com.xie.platform.exception.BizException;
import com.xie.platform.mapper.DepartmentMapper;
import com.xie.platform.mapper.PolicyConfigMapper;
import com.xie.platform.model.Department;
import com.xie.platform.model.PolicyConfig;
import com.xie.platform.model.enumValue.DeptType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyConfigServiceImplTest {

    @Mock
    private PolicyConfigMapper policyConfigMapper;

    @Mock
    private DepartmentMapper departmentMapper;

    @InjectMocks
    private PolicyConfigServiceImpl policyConfigService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(policyConfigService, "objectMapper", new ObjectMapper());
        when(departmentMapper.selectByDeptType(DeptType.MANAGEMENT)).thenReturn(buildManagementDepartment(99L));
    }

    @Test
    void listEditableConfigs_shouldRejectNonPolicyAdmin() {
        BizException exception = assertThrows(
                BizException.class,
                () -> policyConfigService.listEditableConfigs(100L)
        );

        assertEquals("Only the policy admin can maintain policy parameters", exception.getMessage());
    }

    @Test
    void updatePolicyConfig_shouldPersistMergedConditions() {
        PolicyConfig existing = new PolicyConfig();
        existing.setPolicyId(1L);
        existing.setPolicyName("EnvironmentAccessPolicy");
        existing.setConditions("{\"workStart\":\"08:00\",\"workEnd\":\"20:00\"}");
        existing.setEnabled(true);
        existing.setEffect("DENY");
        existing.setPriority(20);

        PolicyConfig refreshed = new PolicyConfig();
        refreshed.setPolicyId(1L);
        refreshed.setPolicyName("EnvironmentAccessPolicy");
        refreshed.setConditions("{\"workStart\":\"09:00\",\"workEnd\":\"21:00\"}");
        refreshed.setEnabled(false);
        refreshed.setPriority(20);

        when(policyConfigMapper.selectByPolicyName("EnvironmentAccessPolicy"))
                .thenReturn(existing)
                .thenReturn(refreshed);

        UpdatePolicyConfigDTO dto = new UpdatePolicyConfigDTO();
        dto.setEnabled(false);
        Map<String, Object> conditions = new LinkedHashMap<>();
        conditions.put("workStart", "09:00");
        conditions.put("workEnd", "21:00");
        dto.setConditions(conditions);

        PolicyConfigDTO result = policyConfigService.updatePolicyConfig("EnvironmentAccessPolicy", dto, 99L);

        ArgumentCaptor<PolicyConfig> captor = ArgumentCaptor.forClass(PolicyConfig.class);
        verify(policyConfigMapper).update(captor.capture());
        assertEquals("{\"workStart\":\"09:00\",\"workEnd\":\"21:00\"}", captor.getValue().getConditions());
        assertEquals(false, captor.getValue().getEnabled());
        assertEquals(false, result.getEnabled());
        assertEquals("09:00", result.getConditions().get("workStart"));
        assertEquals("21:00", result.getConditions().get("workEnd"));
    }

    @Test
    void updatePolicyConfig_shouldValidateThresholdOrdering() {
        UpdatePolicyConfigDTO dto = new UpdatePolicyConfigDTO();
        dto.setEnabled(true);
        dto.setConditions(Map.of(
                "publicMinRank", 4,
                "internalMinRank", 3
        ));

        BizException exception = assertThrows(
                BizException.class,
                () -> policyConfigService.updatePolicyConfig("SecurityLevelPolicy", dto, 99L)
        );

        assertEquals(
                "Security thresholds must satisfy PUBLIC <= INTERNAL <= CONFIDENTIAL <= TOP_SECRET",
                exception.getMessage()
        );
    }

    private Department buildManagementDepartment(Long managerId) {
        Department department = new Department();
        department.setDeptId(1L);
        department.setDeptType(DeptType.MANAGEMENT);
        department.setManagerId(managerId);
        return department;
    }
}
