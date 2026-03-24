package com.xie.platform.controller;

import com.xie.platform.common.Response;
import com.xie.platform.dto.AssetQueryDTO;
import com.xie.platform.dto.CreateAssetDTO;
import com.xie.platform.dto.UploadAssetDTO;
import com.xie.platform.model.ProjectAssets;
import com.xie.platform.service.ProjectAssetsService;
import com.xie.platform.utils.CurrentUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<Long> uploadAsset(
            @ModelAttribute UploadAssetDTO dto,
            @RequestParam("file") MultipartFile file) {
        Long employeeId = CurrentUserContext.getRequiredEmployeeId();
        Long assetId = projectAssetsService.uploadAsset(dto, file, employeeId);
        return Response.Success(assetId, "文件上传成功");
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

    @GetMapping("/{id}/export")
    public Response<Map<String, Object>> exportAssetReference(@PathVariable("id") Long assetId) {
        Long employeeId = CurrentUserContext.getRequiredEmployeeId();
        Map<String, Object> result = projectAssetsService.exportAssetReference(assetId, employeeId);
        return Response.Success(result, "已获取受控文件引用");
    }

    @DeleteMapping("/{id}")
    public Response<Void> deleteAsset(@PathVariable("id") Long assetId) {
        Long employeeId = CurrentUserContext.getRequiredEmployeeId();
        projectAssetsService.deleteAsset(assetId, employeeId);
        return Response.Success(null, "资产删除成功");
    }
}
