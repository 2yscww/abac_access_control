package com.xie.platform.access.policy.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.environment.Environment;
import com.xie.platform.access.policy.PolicyResult;
import com.xie.platform.access.policy.config.SecurityLevelPolicyConfig;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.access.resource.ResourceType;
import com.xie.platform.access.subject.Subject;
import com.xie.platform.model.enumValue.DeptType;
import com.xie.platform.model.enumValue.EmployeeLevel;
import com.xie.platform.model.enumValue.ProjectPhase;
import com.xie.platform.model.enumValue.SecurityLevel;
import com.xie.platform.service.PolicyConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityLevelPolicyTest {

    private final SecurityLevelPolicy policy = new SecurityLevelPolicy();

    @Test
    void evaluate_shouldUseConfiguredSecurityThresholds() {
        PolicyConfigService policyConfigService = mock(PolicyConfigService.class);
        SecurityLevelPolicyConfig config = new SecurityLevelPolicyConfig();
        config.setConfidentialMinRank(EmployeeLevel.P4.getRank());
        when(policyConfigService.getSecurityLevelPolicyConfig()).thenReturn(config);
        ReflectionTestUtils.setField(policy, "policyConfigService", policyConfigService);

        PolicyResult result = policy.evaluate(
                new Subject(7L, 2L, DeptType.RD, 1L, EmployeeLevel.P4, false),
                buildConfidentialProject(),
                Action.READ,
                Environment.builder().build()
        );

        assertEquals(PolicyResult.ALLOW, result);
    }

    private Resource buildConfidentialProject() {
        return Resource.builder()
                .type(ResourceType.PROJECT)
                .resourceId(11L)
                .projectId(11L)
                .projectPhase(ProjectPhase.DEVELOPMENT)
                .securityLevel(SecurityLevel.CONFIDENTIAL)
                .build();
    }
}
