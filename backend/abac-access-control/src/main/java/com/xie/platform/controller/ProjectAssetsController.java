package com.xie.platform.controller;

import com.xie.platform.common.Response;
import com.xie.platform.dto.AssetQueryDTO;
import com.xie.platform.dto.CreateAssetDTO;
import com.xie.platform.model.ProjectAssets;
import com.xie.platform.service.ProjectAssetsService;
import com.xie.platform.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 项目资产管理 Controller
 */
@RestController
@RequestMapping("/api/asset")
public class ProjectAssetsController {

    @Autowired
    private ProjectAssetsService projectAssetsService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 创建资产
     * POST /api/asset/create
     */
    @PostMapping("/create")
    public Response<Long> createAsset(
            @RequestBody CreateAssetDTO dto,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 从 JWT 中解析 employeeId
        Long employeeId = extractEmployeeIdFromToken(authHeader);
        if (employeeId == null) {
            return Response.Fail(null, "未登录或token无效");
        }

        Long assetId = projectAssetsService.createAsset(dto, employeeId);
        return Response.Success(assetId, "资产创建成功");
    }

    /**
     * 查询资产详情
     * GET /api/asset/{id}
     */
    @GetMapping("/{id}")
    public Response<ProjectAssets> getAsset(
            @PathVariable("id") Long assetId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 从 JWT 中解析 employeeId
        Long employeeId = extractEmployeeIdFromToken(authHeader);
        if (employeeId == null) {
            return Response.Fail(null, "未登录或token无效");
        }

        ProjectAssets asset = projectAssetsService.getAssetById(assetId, employeeId);
        return Response.Success(asset, null);
    }

    /**
     * 根据项目ID查询资产列表
     * GET /api/asset/project/{projectId}
     */
    @GetMapping("/project/{projectId}")
    public Response<List<ProjectAssets>> getAssetsByProject(@PathVariable("projectId") Long projectId) {
        List<ProjectAssets> assets = projectAssetsService.getAssetsByProjectId(projectId);
        return Response.Success(assets, null);
    }

    /**
     * 条件查询资产列表（分页）
     * GET /api/asset/list
     */
    @GetMapping("/list")
    public Response<Map<String, Object>> queryAssets(AssetQueryDTO query) {
        Map<String, Object> result = projectAssetsService.queryAssets(query);
        return Response.Success(result, null);
    }

    /**
     * 删除资产
     * DELETE /api/asset/{id}
     */
    @DeleteMapping("/{id}")
    public Response<Void> deleteAsset(
            @PathVariable("id") Long assetId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 从 JWT 中解析 employeeId
        Long employeeId = extractEmployeeIdFromToken(authHeader);
        if (employeeId == null) {
            return Response.Fail(null, "未登录或token无效");
        }

        projectAssetsService.deleteAsset(assetId, employeeId);
        return Response.Success(null, "资产删除成功");
    }

    /**
     * 从 Authorization 请求头中提取 employeeId
     *
     * @param authHeader Authorization 请求头（格式：Bearer <token>）
     * @return employeeId，解析失败返回 null
     */
    private Long extractEmployeeIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        try {
            String token = authHeader.substring(7);
            Claims claims = jwtUtil.parseToken(token);
            String subject = claims.getSubject();
            return Long.parseLong(subject);
        } catch (Exception e) {
            return null;
        }
    }
}
