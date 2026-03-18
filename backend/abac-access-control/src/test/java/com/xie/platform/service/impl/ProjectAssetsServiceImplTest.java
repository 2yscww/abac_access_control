package com.xie.platform.service.impl;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.pep.PolicyEnforcementPoint;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.dto.CreateAssetDTO;
import com.xie.platform.exception.BizException;
import com.xie.platform.mapper.ProjectAssetsMapper;
import com.xie.platform.mapper.ProjectMapper;
import com.xie.platform.model.ProjectAssets;
import com.xie.platform.model.Projects;
import com.xie.platform.model.enumValue.ProjectPhase;
import com.xie.platform.model.enumValue.SecurityLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectAssetsServiceImplTest {

    @Mock
    private ProjectAssetsMapper projectAssetsMapper;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private PolicyEnforcementPoint pep;

    @InjectMocks
    private ProjectAssetsServiceImpl projectAssetsService;

    @Test
    void createAsset_shouldRejectHistoricalStageForNormalCreation() {
        CreateAssetDTO dto = buildCreateAssetDto();
        dto.setAssetsStage(ProjectPhase.INIT.getCode());

        Projects project = new Projects();
        project.setProjectId(dto.getProjectId());
        project.setProjectPhase(ProjectPhase.DEVELOPMENT);
        project.setSecurityLevel(SecurityLevel.INTERNAL);

        when(projectMapper.selectById(dto.getProjectId())).thenReturn(project);

        BizException exception = assertThrows(
                BizException.class,
                () -> projectAssetsService.createAsset(dto, 7L)
        );

        assertEquals("资产产生阶段必须与当前项目阶段一致", exception.getMessage());
        verifyNoInteractions(pep);
        verify(projectAssetsMapper, never()).insert(any(ProjectAssets.class));
    }

    @Test
    void createAsset_shouldPassAssetsStageIntoPepResource() {
        CreateAssetDTO dto = buildCreateAssetDto();
        dto.setAssetsStage(ProjectPhase.DEVELOPMENT.getCode());

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
}
