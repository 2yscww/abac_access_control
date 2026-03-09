package com.xie.platform.service;

import com.xie.platform.dto.AssetQueryDTO;
import com.xie.platform.dto.CreateAssetDTO;
import com.xie.platform.model.ProjectAssets;

import java.util.List;
import java.util.Map;

public interface ProjectAssetsService {

    /**
     * 创建资产
     *
     * @param dto 资产创建信息
     * @param creatorEmployeeId 创建人ID（从JWT中获取）
     * @return 创建的资产ID
     */
    Long createAsset(CreateAssetDTO dto, Long creatorEmployeeId);

    /**
     * 根据ID查询资产详情
     *
     * @param assetId 资产ID
     * @param employeeId 请求员工ID（用于权限检查）
     * @return 资产详情
     */
    ProjectAssets getAssetById(Long assetId, Long employeeId);

    /**
     * 根据项目ID查询资产列表
     *
     * @param projectId 项目ID
     * @return 资产列表
     */
    List<ProjectAssets> getAssetsByProjectId(Long projectId);

    /**
     * 条件查询资产列表（分页）
     *
     * @param query 查询条件
     * @return 资产列表 + 分页信息
     */
    Map<String, Object> queryAssets(AssetQueryDTO query);

    /**
     * 删除资产
     *
     * @param assetId 资产ID
     * @param employeeId 请求员工ID（用于权限检查）
     */
    void deleteAsset(Long assetId, Long employeeId);
}
