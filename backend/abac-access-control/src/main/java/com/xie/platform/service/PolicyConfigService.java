package com.xie.platform.service;

import com.xie.platform.access.policy.config.EnvironmentAccessPolicyConfig;
import com.xie.platform.access.policy.config.HistoricalExportPolicyConfig;
import com.xie.platform.access.policy.config.SecurityLevelPolicyConfig;
import com.xie.platform.dto.PolicyConfigDTO;
import com.xie.platform.dto.UpdatePolicyConfigDTO;

import java.util.List;

public interface PolicyConfigService {

    boolean isPolicyAdmin(Long employeeId);

    SecurityLevelPolicyConfig getSecurityLevelPolicyConfig();

    EnvironmentAccessPolicyConfig getEnvironmentAccessPolicyConfig();

    HistoricalExportPolicyConfig getHistoricalExportPolicyConfig();

    List<PolicyConfigDTO> listRuntimeConfigs();

    List<PolicyConfigDTO> listEditableConfigs(Long operatorEmployeeId);

    PolicyConfigDTO updatePolicyConfig(String policyName, UpdatePolicyConfigDTO dto, Long operatorEmployeeId);
}
