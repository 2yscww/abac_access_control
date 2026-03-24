package com.xie.platform.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String uploadAsset(MultipartFile file, Long projectId, Long employeeId);

    String generateDownloadUrl(String storagePath);

    void delete(String storagePath);

    boolean isManagedPath(String storagePath);
}
