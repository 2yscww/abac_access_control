package com.xie.platform.controller;

import com.xie.platform.common.Response;
import com.xie.platform.dto.PolicyConfigDTO;
import com.xie.platform.dto.UpdatePolicyConfigDTO;
import com.xie.platform.service.PolicyConfigService;
import com.xie.platform.utils.CurrentUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policy-config")
public class PolicyConfigController {

    @Autowired
    private PolicyConfigService policyConfigService;

    @GetMapping("/runtime")
    public Response<List<PolicyConfigDTO>> listRuntimeConfigs() {
        CurrentUserContext.getRequiredEmployeeId();
        return Response.Success(policyConfigService.listRuntimeConfigs(), null);
    }

    @GetMapping
    public Response<List<PolicyConfigDTO>> listEditableConfigs() {
        Long operatorEmployeeId = CurrentUserContext.getRequiredEmployeeId();
        return Response.Success(policyConfigService.listEditableConfigs(operatorEmployeeId), null);
    }

    @PutMapping("/{policyName}")
    public Response<PolicyConfigDTO> updatePolicyConfig(
            @PathVariable String policyName,
            @RequestBody UpdatePolicyConfigDTO dto) {
        Long operatorEmployeeId = CurrentUserContext.getRequiredEmployeeId();
        PolicyConfigDTO data = policyConfigService.updatePolicyConfig(policyName, dto, operatorEmployeeId);
        return Response.Success(data, "Policy config updated");
    }
}
