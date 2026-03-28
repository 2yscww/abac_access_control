package com.xie.platform.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xie.platform.access.action.Action;
import com.xie.platform.access.policy.config.EnvironmentAccessPolicyConfig;
import com.xie.platform.access.policy.config.HistoricalExportPolicyConfig;
import com.xie.platform.access.policy.config.SecurityLevelPolicyConfig;
import com.xie.platform.access.policy.config.ToggleablePolicyConfig;
import com.xie.platform.dto.PolicyConfigDTO;
import com.xie.platform.dto.UpdatePolicyConfigDTO;
import com.xie.platform.exception.BizException;
import com.xie.platform.mapper.DepartmentMapper;
import com.xie.platform.mapper.PolicyConfigMapper;
import com.xie.platform.model.Department;
import com.xie.platform.model.PolicyConfig;
import com.xie.platform.model.enumValue.DeptType;
import com.xie.platform.service.AuditLogService;
import com.xie.platform.service.PolicyConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class PolicyConfigServiceImpl implements PolicyConfigService {

    private static final Logger log = LoggerFactory.getLogger(PolicyConfigServiceImpl.class);
    private static final String SECURITY_LEVEL_POLICY = "SecurityLevelPolicy";
    private static final String ENVIRONMENT_ACCESS_POLICY = "EnvironmentAccessPolicy";
    private static final String HISTORICAL_EXPORT_POLICY = "HistoricalExportPolicy";
    private static final List<String> POLICY_NAMES = List.of(
            SECURITY_LEVEL_POLICY,
            ENVIRONMENT_ACCESS_POLICY,
            HISTORICAL_EXPORT_POLICY
    );
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    @Autowired
    private PolicyConfigMapper policyConfigMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogService auditLogService;

    private final Map<String, PolicyConfig> policyCache = new ConcurrentHashMap<>();
    private final Set<String> missingPolicyNames = ConcurrentHashMap.newKeySet();

    @Override
    @Transactional(readOnly = true)
    public boolean isPolicyAdmin(Long employeeId) {
        if (employeeId == null) {
            return false;
        }

        Department managementDepartment = departmentMapper.selectByDeptType(DeptType.MANAGEMENT);
        return managementDepartment != null
                && Objects.equals(managementDepartment.getManagerId(), employeeId);
    }

    @Override
    @Transactional(readOnly = true)
    public SecurityLevelPolicyConfig getSecurityLevelPolicyConfig() {
        return loadConfig(SECURITY_LEVEL_POLICY, SecurityLevelPolicyConfig.class, SecurityLevelPolicyConfig::new);
    }

    @Override
    @Transactional(readOnly = true)
    public EnvironmentAccessPolicyConfig getEnvironmentAccessPolicyConfig() {
        return loadConfig(
                ENVIRONMENT_ACCESS_POLICY,
                EnvironmentAccessPolicyConfig.class,
                EnvironmentAccessPolicyConfig::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public HistoricalExportPolicyConfig getHistoricalExportPolicyConfig() {
        return loadConfig(
                HISTORICAL_EXPORT_POLICY,
                HistoricalExportPolicyConfig.class,
                HistoricalExportPolicyConfig::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyConfigDTO> listRuntimeConfigs() {
        return POLICY_NAMES.stream()
                .map(this::toPolicyConfigDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyConfigDTO> listEditableConfigs(Long operatorEmployeeId) {
        ensurePolicyAdmin(operatorEmployeeId);
        return listRuntimeConfigs();
    }

    @Override
    @Transactional
    public PolicyConfigDTO updatePolicyConfig(String policyName, UpdatePolicyConfigDTO dto, Long operatorEmployeeId) {
        try {
            ensurePolicyAdmin(operatorEmployeeId);

            if (dto == null) {
                throw new BizException("Policy config payload is required");
            }

            return switch (policyName) {
                case SECURITY_LEVEL_POLICY -> updateTypedConfig(
                        policyName,
                        dto,
                        operatorEmployeeId,
                        SecurityLevelPolicyConfig.class,
                        SecurityLevelPolicyConfig::new
                );
                case ENVIRONMENT_ACCESS_POLICY -> updateTypedConfig(
                        policyName,
                        dto,
                        operatorEmployeeId,
                        EnvironmentAccessPolicyConfig.class,
                        EnvironmentAccessPolicyConfig::new
                );
                case HISTORICAL_EXPORT_POLICY -> updateTypedConfig(
                        policyName,
                        dto,
                        operatorEmployeeId,
                        HistoricalExportPolicyConfig.class,
                        HistoricalExportPolicyConfig::new
                );
                default -> throw new BizException("Unsupported policy name: " + policyName);
            };
        } catch (RuntimeException exception) {
            safeRecordPolicyFailure(operatorEmployeeId, policyName, dto, exception.getMessage());
            throw exception;
        }
    }

    private <T extends ToggleablePolicyConfig> T loadConfig(
            String policyName,
            Class<T> configType,
            Supplier<T> defaultSupplier) {
        T config = defaultSupplier.get();
        PolicyConfig entity = getPolicyEntity(policyName);
        if (entity == null) {
            return config;
        }

        if (entity.getConditions() != null && !entity.getConditions().isBlank()) {
            try {
                objectMapper.readerForUpdating(config).readValue(entity.getConditions());
            } catch (IOException exception) {
                log.warn("Failed to parse policy conditions for {}", policyName, exception);
            }
        }

        if (entity.getEnabled() != null) {
            config.setEnabled(entity.getEnabled());
        }
        return config;
    }

    private <T extends ToggleablePolicyConfig> PolicyConfigDTO updateTypedConfig(
            String policyName,
            UpdatePolicyConfigDTO dto,
            Long operatorEmployeeId,
            Class<T> configType,
            Supplier<T> defaultSupplier) {
        T previousConfig = loadConfig(policyName, configType, defaultSupplier);
        Map<String, Object> oldConditions = objectMapper.convertValue(previousConfig, MAP_TYPE);
        oldConditions.remove("enabled");

        T mergedConfig = loadConfig(policyName, configType, defaultSupplier);
        mergeUpdate(mergedConfig, dto.getConditions());

        if (dto.getEnabled() != null) {
            mergedConfig.setEnabled(dto.getEnabled());
        }
        if (mergedConfig.getEnabled() == null) {
            mergedConfig.setEnabled(true);
        }

        validateConfig(policyName, mergedConfig);

        PolicyConfig entity = getPolicyEntity(policyName);
        PolicyConfig saving = entity != null ? entity : new PolicyConfig();
        saving.setPolicyName(policyName);
        saving.setDescription(saving.getDescription() != null ? saving.getDescription() : defaultDescription(policyName));
        saving.setEffect(saving.getEffect() != null ? saving.getEffect() : "DENY");
        saving.setPriority(saving.getPriority() != null ? saving.getPriority() : defaultPriority(policyName));
        saving.setEnabled(mergedConfig.getEnabled());
        saving.setConditions(serializeConditions(mergedConfig));

        if (entity == null) {
            policyConfigMapper.insert(saving);
        } else {
            policyConfigMapper.update(saving);
        }

        Map<String, Object> detail = new LinkedHashMap<>();
        Map<String, Object> newConditions = objectMapper.convertValue(mergedConfig, MAP_TYPE);
        newConditions.remove("enabled");
        detail.put("operatorEmployeeId", operatorEmployeeId);
        detail.put("policyName", policyName);
        detail.put("oldEnabled", previousConfig.getEnabled());
        detail.put("newEnabled", mergedConfig.getEnabled());
        detail.put("oldConditions", oldConditions);
        detail.put("newConditions", newConditions);

        evictPolicyCache(policyName);
        auditLogService.recordBusinessEvent(
                operatorEmployeeId,
                "POLICY",
                saving.getPolicyId(),
                Action.UPDATE_POLICY_CONFIG,
                detail
        );
        return toPolicyConfigDTO(policyName);
    }

    private void mergeUpdate(ToggleablePolicyConfig target, Map<String, Object> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return;
        }

        Map<String, Object> sanitizedConditions = new LinkedHashMap<>(conditions);
        sanitizedConditions.remove("enabled");
        if (sanitizedConditions.isEmpty()) {
            return;
        }

        try {
            objectMapper.readerForUpdating(target).readValue(objectMapper.writeValueAsBytes(sanitizedConditions));
        } catch (IOException exception) {
            throw new BizException("Invalid policy condition payload");
        }
    }

    private void validateConfig(String policyName, ToggleablePolicyConfig config) {
        switch (policyName) {
            case SECURITY_LEVEL_POLICY -> validateSecurityLevelConfig((SecurityLevelPolicyConfig) config);
            case ENVIRONMENT_ACCESS_POLICY -> validateEnvironmentConfig((EnvironmentAccessPolicyConfig) config);
            case HISTORICAL_EXPORT_POLICY -> validateHistoricalExportConfig((HistoricalExportPolicyConfig) config);
            default -> throw new BizException("Unsupported policy name: " + policyName);
        }
    }

    private void validateSecurityLevelConfig(SecurityLevelPolicyConfig config) {
        requireRank(config.getPublicMinRank(), "PUBLIC");
        requireRank(config.getInternalMinRank(), "INTERNAL");
        requireRank(config.getConfidentialMinRank(), "CONFIDENTIAL");
        requireRank(config.getTopSecretMinRank(), "TOP_SECRET");

        if (config.getPublicMinRank() > config.getInternalMinRank()
                || config.getInternalMinRank() > config.getConfidentialMinRank()
                || config.getConfidentialMinRank() > config.getTopSecretMinRank()) {
            throw new BizException("Security thresholds must satisfy PUBLIC <= INTERNAL <= CONFIDENTIAL <= TOP_SECRET");
        }
    }

    private void validateEnvironmentConfig(EnvironmentAccessPolicyConfig config) {
        LocalTime start = parseTime(config.getWorkStart(), "workStart");
        LocalTime end = parseTime(config.getWorkEnd(), "workEnd");
        if (!start.isBefore(end)) {
            throw new BizException("workStart must be earlier than workEnd");
        }
    }

    private void validateHistoricalExportConfig(HistoricalExportPolicyConfig config) {
        if (config.getExportThreshold() == null || config.getExportThreshold() <= 0) {
            throw new BizException("exportThreshold must be greater than 0");
        }
        if (config.getExportWindowMinutes() == null || config.getExportWindowMinutes() <= 0) {
            throw new BizException("exportWindowMinutes must be greater than 0");
        }
    }

    private void requireRank(Integer rank, String label) {
        if (rank == null || rank < 1 || rank > 10) {
            throw new BizException(label + " min rank must be between 1 and 10");
        }
    }

    private LocalTime parseTime(String value, String label) {
        try {
            return LocalTime.parse(value, TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new BizException(label + " must use HH:mm format");
        }
    }

    private String serializeConditions(ToggleablePolicyConfig config) {
        Map<String, Object> conditionMap = objectMapper.convertValue(config, MAP_TYPE);
        conditionMap.remove("enabled");
        try {
            return objectMapper.writeValueAsString(conditionMap);
        } catch (JsonProcessingException exception) {
            throw new BizException("Failed to persist policy config");
        }
    }

    private PolicyConfigDTO toPolicyConfigDTO(String policyName) {
        return switch (policyName) {
            case SECURITY_LEVEL_POLICY -> buildPolicyConfigDTO(
                    policyName,
                    getSecurityLevelPolicyConfig(),
                    getPolicyEntity(policyName)
            );
            case ENVIRONMENT_ACCESS_POLICY -> buildPolicyConfigDTO(
                    policyName,
                    getEnvironmentAccessPolicyConfig(),
                    getPolicyEntity(policyName)
            );
            case HISTORICAL_EXPORT_POLICY -> buildPolicyConfigDTO(
                    policyName,
                    getHistoricalExportPolicyConfig(),
                    getPolicyEntity(policyName)
            );
            default -> throw new BizException("Unsupported policy name: " + policyName);
        };
    }

    private PolicyConfigDTO buildPolicyConfigDTO(
            String policyName,
            ToggleablePolicyConfig config,
            PolicyConfig entity) {
        PolicyConfigDTO dto = new PolicyConfigDTO();
        dto.setPolicyName(policyName);
        dto.setDisplayName(defaultDisplayName(policyName));
        dto.setDescription(entity != null && entity.getDescription() != null
                ? entity.getDescription()
                : defaultDescription(policyName));
        dto.setEnabled(config.getEnabled());
        dto.setPriority(entity != null && entity.getPriority() != null
                ? entity.getPriority()
                : defaultPriority(policyName));

        Map<String, Object> conditions = objectMapper.convertValue(config, MAP_TYPE);
        conditions.remove("enabled");
        dto.setConditions(conditions);
        dto.setUpdatedAt(entity != null ? entity.getUpdatedAt() : null);
        return dto;
    }

    private PolicyConfig getPolicyEntity(String policyName) {
        PolicyConfig cached = policyCache.get(policyName);
        if (cached != null) {
            return cached;
        }
        if (missingPolicyNames.contains(policyName)) {
            return null;
        }

        PolicyConfig entity = policyConfigMapper.selectByPolicyName(policyName);
        if (entity == null) {
            missingPolicyNames.add(policyName);
            return null;
        }

        policyCache.put(policyName, entity);
        missingPolicyNames.remove(policyName);
        return entity;
    }

    private void evictPolicyCache(String policyName) {
        policyCache.remove(policyName);
        missingPolicyNames.remove(policyName);
    }

    private void ensurePolicyAdmin(Long operatorEmployeeId) {
        if (!isPolicyAdmin(operatorEmployeeId)) {
            throw new BizException("Only the policy admin can maintain policy parameters");
        }
    }

    private void safeRecordPolicyFailure(
            Long operatorEmployeeId,
            String policyName,
            UpdatePolicyConfigDTO dto,
            String reason) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("operatorEmployeeId", operatorEmployeeId);
        detail.put("policyName", policyName);
        detail.put("requestedEnabled", dto != null ? dto.getEnabled() : null);
        detail.put("requestedConditions", dto != null ? dto.getConditions() : null);
        detail.put("result", "FAILURE");

        try {
            auditLogService.recordSecurityEvent(
                    operatorEmployeeId,
                    "POLICY",
                    null,
                    Action.UPDATE_POLICY_CONFIG,
                    reason,
                    detail
            );
        } catch (RuntimeException auditException) {
            log.warn("Failed to record denied policy update for {}", policyName, auditException);
        }
    }

    private String defaultDisplayName(String policyName) {
        return switch (policyName) {
            case SECURITY_LEVEL_POLICY -> "Security Thresholds";
            case ENVIRONMENT_ACCESS_POLICY -> "Working Hours";
            case HISTORICAL_EXPORT_POLICY -> "Export Guard";
            default -> policyName;
        };
    }

    private String defaultDescription(String policyName) {
        return switch (policyName) {
            case SECURITY_LEVEL_POLICY ->
                    "Controls minimum employee ranks per security level while keeping policy logic in code.";
            case ENVIRONMENT_ACCESS_POLICY ->
                    "Controls the working-hour window for sensitive access while keeping policy logic in code.";
            case HISTORICAL_EXPORT_POLICY ->
                    "Controls export frequency thresholds while keeping policy logic in code.";
            default -> "Policy configuration";
        };
    }

    private int defaultPriority(String policyName) {
        return switch (policyName) {
            case SECURITY_LEVEL_POLICY -> 10;
            case ENVIRONMENT_ACCESS_POLICY -> 20;
            case HISTORICAL_EXPORT_POLICY -> 30;
            default -> 0;
        };
    }
}
