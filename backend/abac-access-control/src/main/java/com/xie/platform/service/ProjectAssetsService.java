package com.xie.platform.service;

import com.xie.platform.dto.AssetQueryDTO;
import com.xie.platform.dto.CreateAssetDTO;
import com.xie.platform.dto.UploadAssetDTO;
import com.xie.platform.model.ProjectAssets;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ProjectAssetsService {

    Long createAsset(CreateAssetDTO dto, Long creatorEmployeeId);

    Long uploadAsset(UploadAssetDTO dto, MultipartFile file, Long creatorEmployeeId);

    ProjectAssets getAssetById(Long assetId, Long employeeId);

    List<ProjectAssets> getAssetsByProjectId(Long projectId, Long employeeId);

    Map<String, Object> queryAssets(AssetQueryDTO query, Long employeeId);

    Map<String, Object> exportAssetReference(Long assetId, Long employeeId);

    void deleteAsset(Long assetId, Long employeeId);
}
