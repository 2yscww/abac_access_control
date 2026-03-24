package com.xie.platform.service.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.pep.PolicyEnforcementPoint;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.access.resource.ResourceType;
import com.xie.platform.dto.AssetQueryDTO;
import com.xie.platform.dto.CreateAssetDTO;
import com.xie.platform.dto.UploadAssetDTO;
import com.xie.platform.exception.BizException;
import com.xie.platform.mapper.ProjectAssetsMapper;
import com.xie.platform.mapper.ProjectMapper;
import com.xie.platform.model.ProjectAssets;
import com.xie.platform.model.Projects;
import com.xie.platform.model.enumValue.AssetType;
import com.xie.platform.model.enumValue.ProjectPhase;
import com.xie.platform.model.enumValue.SecurityLevel;
import com.xie.platform.service.FileStorageService;
import com.xie.platform.service.ProjectAssetsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProjectAssetsServiceImpl implements ProjectAssetsService {

    @Autowired
    private ProjectAssetsMapper projectAssetsMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private PolicyEnforcementPoint pep;

    @Autowired
    private FileStorageService fileStorageService;

    @Override
    @Transactional
    public Long createAsset(CreateAssetDTO dto, Long creatorEmployeeId) {
        ProjectAssets asset = buildValidatedAsset(dto, creatorEmployeeId);
        projectAssetsMapper.insert(asset);
        return asset.getAssetId();
    }

    @Override
    @Transactional
    public Long uploadAsset(UploadAssetDTO dto, MultipartFile file, Long creatorEmployeeId) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件不能为空");
        }

        ProjectAssets asset = buildValidatedAsset(toCreateAssetDTO(dto), creatorEmployeeId);
        String storagePath = fileStorageService.uploadAsset(file, asset.getProjectId(), creatorEmployeeId);

        asset.setFilePath(storagePath);
        asset.setFileSize(file.getSize());

        try {
            projectAssetsMapper.insert(asset);
        } catch (RuntimeException exception) {
            fileStorageService.delete(storagePath);
            throw exception;
        }

        return asset.getAssetId();
    }

    @Override
    public ProjectAssets getAssetById(Long assetId, Long employeeId) {
        if (assetId == null) {
            throw new BizException("资产ID不能为空");
        }

        pep.checkAssetAccess(employeeId, assetId, Action.READ);

        ProjectAssets asset = projectAssetsMapper.selectById(assetId);
        if (asset == null) {
            throw new BizException("资产不存在");
        }
        return maskAssetReference(asset);
    }

    @Override
    public List<ProjectAssets> getAssetsByProjectId(Long projectId, Long employeeId) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }

        pep.checkProjectAccess(employeeId, projectId, Action.READ);

        return projectAssetsMapper.selectByProjectId(projectId).stream()
                .filter(asset -> pep.decideAssetAccess(employeeId, asset.getAssetId(), Action.READ).isAllowed())
                .map(this::maskAssetReference)
                .toList();
    }

    @Override
    public Map<String, Object> queryAssets(AssetQueryDTO query, Long employeeId) {
        AssetQueryDTO safeQuery = query == null ? new AssetQueryDTO() : query;
        int pageNum = normalizePageNum(safeQuery.getPageNum());
        int pageSize = normalizePageSize(safeQuery.getPageSize());

        List<ProjectAssets> matchedAssets = projectAssetsMapper.selectByCondition(copyQueryWithoutPagination(safeQuery));
        List<ProjectAssets> accessibleAssets = matchedAssets.stream()
                .filter(asset -> pep.decideAssetAccess(employeeId, asset.getAssetId(), Action.READ).isAllowed())
                .map(this::maskAssetReference)
                .toList();

        return buildPageResult(accessibleAssets, pageNum, pageSize);
    }

    @Override
    public Map<String, Object> exportAssetReference(Long assetId, Long employeeId) {
        if (assetId == null) {
            throw new BizException("资产ID不能为空");
        }

        pep.checkAssetAccess(employeeId, assetId, Action.EXPORT);

        ProjectAssets asset = projectAssetsMapper.selectById(assetId);
        if (asset == null) {
            throw new BizException("资产不存在");
        }
        if (!StringUtils.hasText(asset.getFilePath())) {
            throw new BizException("该资产未配置外部地址引用");
        }

        boolean managedByMinio = fileStorageService.isManagedPath(asset.getFilePath());
        String exportedPath = managedByMinio
                ? fileStorageService.generateDownloadUrl(asset.getFilePath())
                : asset.getFilePath();

        Map<String, Object> result = new HashMap<>();
        result.put("assetId", asset.getAssetId());
        result.put("assetName", asset.getAssetName());
        result.put("filePath", exportedPath);
        result.put("downloadUrl", exportedPath);
        result.put("storagePath", asset.getFilePath());
        result.put("storageType", managedByMinio ? "MINIO" : "EXTERNAL_URL");
        return result;
    }

    @Override
    @Transactional
    public void deleteAsset(Long assetId, Long employeeId) {
        if (assetId == null) {
            throw new BizException("资产ID不能为空");
        }

        pep.checkAssetAccess(employeeId, assetId, Action.DELETE);

        ProjectAssets asset = projectAssetsMapper.selectById(assetId);
        if (asset == null) {
            throw new BizException("资产不存在");
        }

        projectAssetsMapper.deleteById(assetId);
        fileStorageService.delete(asset.getFilePath());
    }

    private ProjectAssets buildValidatedAsset(CreateAssetDTO dto, Long creatorEmployeeId) {
        if (dto == null) {
            throw new BizException("资产请求不能为空");
        }
        if (dto.getProjectId() == null) {
            throw new BizException("项目ID不能为空");
        }
        if (!StringUtils.hasText(dto.getAssetName())) {
            throw new BizException("资产名称不能为空");
        }
        if (dto.getAssetsType() == null) {
            throw new BizException("资产类型不能为空");
        }
        if (dto.getAssetsStage() == null) {
            throw new BizException("资产产生阶段不能为空");
        }
        if (dto.getSecurityLevel() == null) {
            throw new BizException("资产密级不能为空");
        }

        Projects project = projectMapper.selectById(dto.getProjectId());
        if (project == null) {
            throw new BizException("项目不存在");
        }

        AssetType assetType;
        try {
            assetType = AssetType.fromCode(dto.getAssetsType());
        } catch (IllegalArgumentException exception) {
            throw new BizException("非法的资产类型");
        }

        ProjectPhase assetsStage;
        try {
            assetsStage = ProjectPhase.fromCode(dto.getAssetsStage());
        } catch (IllegalArgumentException exception) {
            throw new BizException("非法的资产产生阶段");
        }

        if (assetsStage.getCode() > project.getProjectPhase().getCode()) {
            throw new BizException("资产产生阶段不能晚于当前项目阶段");
        }

        SecurityLevel securityLevel;
        try {
            securityLevel = SecurityLevel.fromLevel(dto.getSecurityLevel());
        } catch (IllegalArgumentException exception) {
            throw new BizException("非法的资产密级");
        }

        pep.checkAccess(
                creatorEmployeeId,
                Resource.builder()
                        .type(ResourceType.ASSET)
                        .projectId(dto.getProjectId())
                        .projectPhase(project.getProjectPhase())
                        .assetsStage(assetsStage)
                        .securityLevel(securityLevel)
                        .creatorId(creatorEmployeeId)
                        .build(),
                Action.WRITE
        );

        ProjectAssets asset = new ProjectAssets();
        asset.setProjectId(dto.getProjectId());
        asset.setAssetName(dto.getAssetName().trim());
        asset.setAssetsType(assetType);
        asset.setAssetsStage(assetsStage);
        asset.setSecurityLevel(securityLevel);
        asset.setCreatedByEmployeeId(creatorEmployeeId);
        asset.setFilePath(normalizeNullableText(dto.getFilePath()));
        asset.setFileSize(dto.getFileSize());
        asset.setDescription(normalizeNullableText(dto.getDescription()));
        return asset;
    }

    private CreateAssetDTO toCreateAssetDTO(UploadAssetDTO dto) {
        CreateAssetDTO createAssetDTO = new CreateAssetDTO();
        createAssetDTO.setProjectId(dto.getProjectId());
        createAssetDTO.setAssetName(dto.getAssetName());
        createAssetDTO.setAssetsType(dto.getAssetsType());
        createAssetDTO.setAssetsStage(dto.getAssetsStage());
        createAssetDTO.setSecurityLevel(dto.getSecurityLevel());
        createAssetDTO.setDescription(dto.getDescription());
        return createAssetDTO;
    }

    private String normalizeNullableText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private AssetQueryDTO copyQueryWithoutPagination(AssetQueryDTO query) {
        AssetQueryDTO copiedQuery = new AssetQueryDTO();
        copiedQuery.setProjectId(query.getProjectId());
        copiedQuery.setAssetName(query.getAssetName());
        copiedQuery.setAssetsType(query.getAssetsType());
        copiedQuery.setAssetsStage(query.getAssetsStage());
        copiedQuery.setSecurityLevel(query.getSecurityLevel());
        copiedQuery.setCreatedByEmployeeId(query.getCreatedByEmployeeId());
        copiedQuery.setPageNum(null);
        copiedQuery.setPageSize(null);
        return copiedQuery;
    }

    private Map<String, Object> buildPageResult(List<?> data, int pageNum, int pageSize) {
        int total = data.size();
        int fromIndex = Math.min((pageNum - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);

        Map<String, Object> result = new HashMap<>();
        result.put("list", data.subList(fromIndex, toIndex));
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }

    private ProjectAssets maskAssetReference(ProjectAssets asset) {
        asset.setFilePath(null);
        return asset;
    }
}
