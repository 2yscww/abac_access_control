package com.xie.platform.dto;

import lombok.Data;

import java.util.Map;

@Data
public class UpdatePolicyConfigDTO {
    private Boolean enabled;
    private Map<String, Object> conditions;
}
