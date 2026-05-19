package com.xie.platform.service.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.pdp.DecisionResult;
import com.xie.platform.access.pep.PolicyEnforcementPoint;
import com.xie.platform.access.resource.Resource;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectAssetsServiceImplTest {

    @Mock
    private ProjectAssetsMapper projectAssetsMapper;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private PolicyEnforcementPoint pep;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ProjectAssetsServiceImpl projectAssetsService;

    @Test
    void createAsset_shouldUseCurrentProjectPhaseForPepAndPersistence() {
        CreateAssetDTO dto = buildCreateAssetDto();

        Projects project = new Projects();
        project.setProjectId(dto.getProjectId());
        project.setProjectPhase(ProjectPhase.DEVELOPMENT);
        project.setSecurityLevel(SecurityLevel.INTERNAL);

        when(projectMapper.selectById(dto.getProjectId())).thenReturn(project);
        doAnswer(invocation -> {
            ProjectAssets asset = invocation.getArgument(0);
            asset.setAssetId(99L);
            return 1;
        }).when(projectAssetsMapper).insert(any(ProjectAssets.class));

        Long assetId = projectAssetsService.createAsset(dto, 7L);

        assertEquals(99L, assetId);

        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(pep).checkAccess(eq(7L), resourceCaptor.capture(), eq(Action.WRITE));

        Resource resource = resourceCaptor.getValue();
        assertEquals(ProjectPhase.DEVELOPMENT, resource.getProjectPhase());
        assertEquals(ProjectPhase.DEVELOPMENT, resource.getAssetsStage());
        assertEquals(SecurityLevel.INTERNAL, resource.getSecurityLevel());
        assertEquals(AssetType.REQUIREMENT_DOC, resource.getAssetType());

        ArgumentCaptor<ProjectAssets> assetCaptor = ArgumentCaptor.forClass(ProjectAssets.class);
        verify(projectAssetsMapper).insert(assetCaptor.capture());
        assertEquals(ProjectPhase.DEVELOPMENT, assetCaptor.getValue().getAssetsStage());
    }

    @Test
    void uploadAsset_shouldPersistStoragePathActualFileSizeAndCurrentProjectPhase() {
        UploadAssetDTO dto = buildUploadAssetDto();
        Projects project = new Projects();
        project.setProjectId(dto.getProjectId());
        project.setProjectPhase(ProjectPhase.DEVELOPMENT);
        project.setSecurityLevel(SecurityLevel.INTERNAL);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "budget-sheet.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "demo-content".getBytes()
        );

        when(projectMapper.selectById(dto.getProjectId())).thenReturn(project);
        when(fileStorageService.uploadAsset(any(), eq(dto.getProjectId()), eq(7L)))
                .thenReturn("minio://abac-assets/project-assets/1/demo.xlsx");
        doAnswer(invocation -> {
            ProjectAssets asset = invocation.getArgument(0);
            asset.setAssetId(101L);
            return 1;
        }).when(projectAssetsMapper).insert(any(ProjectAssets.class));

        Long assetId = projectAssetsService.uploadAsset(dto, file, 7L);

        assertEquals(101L, assetId);

        ArgumentCaptor<ProjectAssets> assetCaptor = ArgumentCaptor.forClass(ProjectAssets.class);
        verify(projectAssetsMapper).insert(assetCaptor.capture());
        assertEquals("minio://abac-assets/project-assets/1/demo.xlsx", assetCaptor.getValue().getFilePath());
        assertEquals(file.getSize(), assetCaptor.getValue().getFileSize());
        assertEquals(ProjectPhase.DEVELOPMENT, assetCaptor.getValue().getAssetsStage());
        assertEquals(AssetType.REQUIREMENT_DOC, assetCaptor.getValue().getAssetsType());
    }

    @Test
    void getAssetById_shouldMaskFilePath() {
        ProjectAssets asset = buildAsset(21L, 11L, "oss://bucket/project/budget-sheet.xlsx");

        when(projectAssetsMapper.selectById(21L)).thenReturn(asset);

        ProjectAssets result = projectAssetsService.getAssetById(21L, 7L);

        assertEquals(21L, result.getAssetId());
        assertNull(result.getFilePath());
        verify(pep).checkAssetAccess(7L, 21L, Action.READ);
    }

    @Test
    void getAssetsByProjectId_shouldFilterDeniedAssetAndMaskFilePath() {
        ProjectAssets allowedAsset = buildAsset(21L, 11L, "oss://bucket/project/allowed.docx");
        ProjectAssets deniedAsset = buildAsset(22L, 11L, "oss://bucket/project/denied.docx");

        when(projectAssetsMapper.selectByProjectId(11L)).thenReturn(List.of(allowedAsset, deniedAsset));
        when(pep.decideAssetAccess(7L, 21L, Action.READ)).thenReturn(DecisionResult.allow());
        when(pep.decideAssetAccess(7L, 22L, Action.READ))
                .thenReturn(DecisionResult.deny("SecurityLevelPolicy", "denied"));

        List<ProjectAssets> result = projectAssetsService.getAssetsByProjectId(11L, 7L);

        assertEquals(1, result.size());
        assertEquals(21L, result.get(0).getAssetId());
        assertNull(result.get(0).getFilePath());
        verify(pep).checkProjectAccess(7L, 11L, Action.READ);
    }

    @Test
    void exportAssetReference_shouldCheckExportAccessAndReturnExternalFilePath() {
        ProjectAssets asset = buildAsset(21L, 11L, "gitlab://group/project/repo");

        when(projectAssetsMapper.selectById(21L)).thenReturn(asset);
        when(fileStorageService.isManagedPath("gitlab://group/project/repo")).thenReturn(false);

        Map<String, Object> result = projectAssetsService.exportAssetReference(21L, 7L);

        assertEquals(21L, result.get("assetId"));
        assertEquals("design-doc", result.get("assetName"));
        assertEquals("gitlab://group/project/repo", result.get("filePath"));
        assertEquals("EXTERNAL_URL", result.get("storageType"));
        verify(pep).checkAssetAccess(7L, 21L, Action.EXPORT);
    }

    @Test
    void exportAssetReference_shouldReturnPresignedUrlForManagedPath() {
        ProjectAssets asset = buildAsset(22L, 11L, "minio://abac-assets/project-assets/1/demo.xlsx");

        when(projectAssetsMapper.selectById(22L)).thenReturn(asset);
        when(fileStorageService.isManagedPath("minio://abac-assets/project-assets/1/demo.xlsx")).thenReturn(true);
        when(fileStorageService.generateDownloadUrl("minio://abac-assets/project-assets/1/demo.xlsx"))
                .thenReturn("https://minio.example.com/presigned/demo");

        Map<String, Object> result = projectAssetsService.exportAssetReference(22L, 7L);

        assertEquals("https://minio.example.com/presigned/demo", result.get("filePath"));
        assertEquals("https://minio.example.com/presigned/demo", result.get("downloadUrl"));
        assertEquals("MINIO", result.get("storageType"));
    }

    @Test
    void deleteAsset_shouldDeleteMetadataAndManagedFile() {
        ProjectAssets asset = buildAsset(22L, 11L, "minio://abac-assets/project-assets/1/demo.xlsx");

        when(projectAssetsMapper.selectById(22L)).thenReturn(asset);

        projectAssetsService.deleteAsset(22L, 7L);

        verify(pep).checkAssetAccess(7L, 22L, Action.DELETE);
        verify(projectAssetsMapper).deleteById(22L);
        verify(fileStorageService).delete("minio://abac-assets/project-assets/1/demo.xlsx");
    }

    @Test
    void exportAssetReference_shouldRejectAssetWithoutFileReference() {
        ProjectAssets asset = buildAsset(23L, 11L, null);

        when(projectAssetsMapper.selectById(23L)).thenReturn(asset);

        BizException exception = assertThrows(
                BizException.class,
                () -> projectAssetsService.exportAssetReference(23L, 7L)
        );

        assertEquals("该资产未配置外部地址引用", exception.getMessage());
        verify(pep).checkAssetAccess(7L, 23L, Action.EXPORT);
        verify(fileStorageService, never()).generateDownloadUrl(any());
    }

    private CreateAssetDTO buildCreateAssetDto() {
        CreateAssetDTO dto = new CreateAssetDTO();
        dto.setProjectId(1L);
        dto.setAssetName("budget-sheet");
        dto.setAssetsType(1);
        dto.setSecurityLevel(SecurityLevel.INTERNAL.getLevel());
        dto.setFilePath("oss://bucket/project/budget-sheet.xlsx");
        dto.setDescription("external reference");
        return dto;
    }

    private UploadAssetDTO buildUploadAssetDto() {
        UploadAssetDTO dto = new UploadAssetDTO();
        dto.setProjectId(1L);
        dto.setAssetName("budget-sheet");
        dto.setAssetsType(1);
        dto.setSecurityLevel(SecurityLevel.INTERNAL.getLevel());
        dto.setDescription("uploaded into minio");
        return dto;
    }

    private ProjectAssets buildAsset(Long assetId, Long projectId, String filePath) {
        ProjectAssets asset = new ProjectAssets();
        asset.setAssetId(assetId);
        asset.setProjectId(projectId);
        asset.setAssetName("design-doc");
        asset.setAssetsStage(ProjectPhase.DEVELOPMENT);
        asset.setSecurityLevel(SecurityLevel.INTERNAL);
        asset.setFilePath(filePath);
        return asset;
    }
}
