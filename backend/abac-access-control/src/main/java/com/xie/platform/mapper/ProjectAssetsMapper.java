package com.xie.platform.mapper;

import com.xie.platform.dto.AssetQueryDTO;
import com.xie.platform.model.ProjectAssets;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProjectAssetsMapper {

    /**
     * 插入资产
     */
    int insert(ProjectAssets asset);

    /**
     * 根据资产ID查询
     */
    ProjectAssets selectById(@Param("assetId") Long assetId);

    /**
     * 根据项目ID查询资产列表
     */
    List<ProjectAssets> selectByProjectId(@Param("projectId") Long projectId);

    /**
     * 条件查询资产列表（分页）
     */
    List<ProjectAssets> selectByCondition(AssetQueryDTO query);

    /**
     * 统计符合条件的资产数量
     */
    int countByCondition(AssetQueryDTO query);

    /**
     * 删除资产（物理删除）
     */
    int deleteById(@Param("assetId") Long assetId);

    /**
     * 根据项目ID删除所有资产
     */
    int deleteByProjectId(@Param("projectId") Long projectId);
}
