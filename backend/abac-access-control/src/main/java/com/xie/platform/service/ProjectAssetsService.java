package com.xie.platform.service;

import com.xie.platform.dto.AssetQueryDTO;
import com.xie.platform.dto.CreateAssetDTO;
import com.xie.platform.model.ProjectAssets;

import java.util.List;
import java.util.Map;

public interface ProjectAssetsService {

    Long createAsset(CreateAssetDTO dto, Long creatorEmployeeId);

    ProjectAssets getAssetById(Long assetId, Long employeeId);

    List<ProjectAssets> getAssetsByProjectId(Long projectId, Long employeeId);

    Map<String, Object> queryAssets(AssetQueryDTO query, Long employeeId);

    void deleteAsset(Long assetId, Long employeeId);
}
