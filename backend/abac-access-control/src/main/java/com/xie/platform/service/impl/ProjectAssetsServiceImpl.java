package com.xie.platform.service.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.pep.PolicyEnforcementPoint;
import com.xie.platform.dto.AssetQueryDTO;
import com.xie.platform.dto.CreateAssetDTO;
import com.xie.platform.exception.BizException;
import com.xie.platform.mapper.ProjectAssetsMapper;
import com.xie.platform.mapper.ProjectMapper;
import com.xie.platform.model.ProjectAssets;
import com.xie.platform.model.Projects;
import com.xie.platform.model.enumValue.AssetType;
import com.xie.platform.model.enumValue.ProjectPhase;
import com.xie.platform.model.enumValue.SecurityLevel;
import com.xie.platform.service.ProjectAssetsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional
    public Long createAsset(CreateAssetDTO dto, Long creatorEmployeeId) {

        // 1. 参数校验
        if (dto.getProjectId() == null) {
            throw new BizException("项目ID不能为空");
        }
        if (dto.getAssetName() == null || dto.getAssetName().isBlank()) {
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

        // 2. 校验项目是否存在
        Projects project = projectMapper.selectById(dto.getProjectId());
        if (project == null) {
            throw new BizException("项目不存在");
        }

        // 3. 校验枚举合法性
        AssetType assetType;
        try {
            assetType = AssetType.fromCode(dto.getAssetsType());
        } catch (IllegalArgumentException e) {
            throw new BizException("非法的资产类型");
        }

        ProjectPhase assetsStage;
        try {
            assetsStage = ProjectPhase.fromCode(dto.getAssetsStage());
        } catch (IllegalArgumentException e) {
            throw new BizException("非法的资产产生阶段");
        }

        SecurityLevel securityLevel;
        try {
            securityLevel = SecurityLevel.fromLevel(dto.getSecurityLevel());
        } catch (IllegalArgumentException e) {
            throw new BizException("非法的资产密级");
        }

        // 4. 构建资产实体
        ProjectAssets asset = new ProjectAssets();
        asset.setProjectId(dto.getProjectId());
        asset.setAssetName(dto.getAssetName());
        asset.setAssetsType(assetType);
        asset.setAssetsStage(assetsStage);
        asset.setSecurityLevel(securityLevel);
        asset.setCreatedByEmployeeId(creatorEmployeeId);
        asset.setFilePath(dto.getFilePath());
        asset.setFileSize(dto.getFileSize());
        asset.setDescription(dto.getDescription());

        // 5. 插入数据库
        projectAssetsMapper.insert(asset);

        return asset.getAssetId();
    }

    @Override
    public ProjectAssets getAssetById(Long assetId, Long employeeId) {
        if (assetId == null) {
            throw new BizException("资产ID不能为空");
        }

        // ABAC 权限检查：检查员工是否有权读取该资产
        pep.checkAssetAccess(employeeId, assetId, Action.READ);

        ProjectAssets asset = projectAssetsMapper.selectById(assetId);
        if (asset == null) {
            throw new BizException("资产不存在");
        }

        return asset;
    }

    @Override
    public List<ProjectAssets> getAssetsByProjectId(Long projectId) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }

        // 校验项目是否存在
        Projects project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BizException("项目不存在");
        }

        return projectAssetsMapper.selectByProjectId(projectId);
    }

    @Override
    public Map<String, Object> queryAssets(AssetQueryDTO query) {

        // 1. 参数校验与默认值
        if (query.getPageNum() == null || query.getPageNum() < 1) {
            query.setPageNum(1);
        }
        if (query.getPageSize() == null || query.getPageSize() < 1) {
            query.setPageSize(10);
        }

        // 2. 计算分页偏移量
        int offset = (query.getPageNum() - 1) * query.getPageSize();
        query.setPageNum(offset);

        // 3. 查询数据
        List<ProjectAssets> assets = projectAssetsMapper.selectByCondition(query);
        int total = projectAssetsMapper.countByCondition(query);

        // 4. 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("list", assets);
        result.put("total", total);
        result.put("pageNum", (offset / query.getPageSize()) + 1);
        result.put("pageSize", query.getPageSize());

        return result;
    }

    @Override
    @Transactional
    public void deleteAsset(Long assetId, Long employeeId) {
        if (assetId == null) {
            throw new BizException("资产ID不能为空");
        }

        // ABAC 权限检查：检查员工是否有权删除该资产
        pep.checkAssetAccess(employeeId, assetId, Action.DELETE);

        ProjectAssets asset = projectAssetsMapper.selectById(assetId);
        if (asset == null) {
            throw new BizException("资产不存在");
        }

        // 删除资产
        projectAssetsMapper.deleteById(assetId);
    }
}
