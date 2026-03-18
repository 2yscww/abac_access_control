package com.xie.platform.controller;

import com.xie.platform.common.Response;
import com.xie.platform.dto.AssetQueryDTO;
import com.xie.platform.dto.CreateAssetDTO;
import com.xie.platform.model.ProjectAssets;
import com.xie.platform.service.ProjectAssetsService;
import com.xie.platform.utils.CurrentUserContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/asset")
public class ProjectAssetsController {

    @Autowired
    private ProjectAssetsService projectAssetsService;

    @PostMapping("/create")
    public Response<Long> createAsset(@RequestBody CreateAssetDTO dto) {
        Long employeeId = CurrentUserContext.getRequiredEmployeeId();
        Long assetId = projectAssetsService.createAsset(dto, employeeId);
        return Response.Success(assetId, "资产创建成功");
    }

    @GetMapping("/{id}")
    public Response<ProjectAssets> getAsset(@PathVariable("id") Long assetId) {
        Long employeeId = CurrentUserContext.getRequiredEmployeeId();
        ProjectAssets asset = projectAssetsService.getAssetById(assetId, employeeId);
        return Response.Success(asset, null);
    }

    @GetMapping("/project/{projectId}")
    public Response<List<ProjectAssets>> getAssetsByProject(@PathVariable("projectId") Long projectId) {
        Long employeeId = CurrentUserContext.getRequiredEmployeeId();
        List<ProjectAssets> assets = projectAssetsService.getAssetsByProjectId(projectId, employeeId);
        return Response.Success(assets, null);
    }

    @GetMapping("/list")
    public Response<Map<String, Object>> queryAssets(AssetQueryDTO query) {
        Long employeeId = CurrentUserContext.getRequiredEmployeeId();
        Map<String, Object> result = projectAssetsService.queryAssets(query, employeeId);
        return Response.Success(result, null);
    }

    @DeleteMapping("/{id}")
    public Response<Void> deleteAsset(@PathVariable("id") Long assetId) {
        Long employeeId = CurrentUserContext.getRequiredEmployeeId();
        projectAssetsService.deleteAsset(assetId, employeeId);
        return Response.Success(null, "资产删除成功");
    }
}
