package com.xie.platform.dto;

import lombok.Data;

@Data
public class UploadAssetDTO {
    private Long projectId;
    private String assetName;
    private Integer assetsType;
    private Integer securityLevel;
    private String description;
}
