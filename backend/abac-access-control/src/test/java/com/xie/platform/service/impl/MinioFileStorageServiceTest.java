package com.xie.platform.service.impl;

import com.xie.platform.config.MinioStorageProperties;
import com.xie.platform.exception.BizException;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinioFileStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private ObjectProvider<MinioClient> minioClientProvider;

    @Test
    void uploadAsset_shouldUploadFileAndReturnManagedPathWhenBucketExists() throws Exception {
        when(minioClientProvider.getIfAvailable()).thenReturn(minioClient);
        when(minioClient.bucketExists(any())).thenReturn(true);
        when(minioClient.putObject(any())).thenReturn(null);

        MinioFileStorageService service = new MinioFileStorageService(buildEnabledProperties(), minioClientProvider);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "design plan?.txt",
                "text/plain",
                "demo-content".getBytes()
        );

        String path = service.uploadAsset(file, 8L, 9L);

        assertTrue(path.startsWith("minio://abac/project-assets/8/"));
        assertTrue(path.endsWith("design_plan_.txt"));
        verify(minioClient).bucketExists(any());
        verify(minioClient).putObject(any());
    }

    @Test
    void uploadAsset_shouldCreateBucketWhenMissingAndAutoCreateEnabled() throws Exception {
        when(minioClientProvider.getIfAvailable()).thenReturn(minioClient);
        when(minioClient.bucketExists(any())).thenReturn(false);
        when(minioClient.putObject(any())).thenReturn(null);

        MinioFileStorageService service = new MinioFileStorageService(buildEnabledProperties(), minioClientProvider);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "design.txt",
                "text/plain",
                "demo-content".getBytes()
        );

        String path = service.uploadAsset(file, 8L, 9L);

        assertTrue(path.startsWith("minio://abac/project-assets/8/"));
        verify(minioClient).makeBucket(any());
        verify(minioClient).putObject(any());
    }

    @Test
    void uploadAsset_shouldRejectMissingBucketWhenAutoCreateDisabled() throws Exception {
        MinioStorageProperties properties = buildEnabledProperties();
        properties.setAutoCreateBucket(false);

        when(minioClientProvider.getIfAvailable()).thenReturn(minioClient);
        when(minioClient.bucketExists(any())).thenReturn(false);

        MinioFileStorageService service = new MinioFileStorageService(properties, minioClientProvider);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "design.txt",
                "text/plain",
                "demo-content".getBytes()
        );

        BizException exception = assertThrows(
                BizException.class,
                () -> service.uploadAsset(file, 8L, 9L)
        );

        assertEquals("MinIO Bucket 不存在，请先手动创建: abac", exception.getMessage());
        verify(minioClient, never()).putObject(any());
    }

    @Test
    void uploadAsset_shouldRejectEmptyFile() throws Exception {
        when(minioClientProvider.getIfAvailable()).thenReturn(minioClient);
        when(minioClient.bucketExists(any())).thenReturn(true);

        MinioFileStorageService service = new MinioFileStorageService(buildEnabledProperties(), minioClientProvider);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        BizException exception = assertThrows(
                BizException.class,
                () -> service.uploadAsset(file, 8L, 9L)
        );

        assertEquals("上传文件不能为空", exception.getMessage());
        verify(minioClient, never()).putObject(any());
    }

    @Test
    void uploadAsset_shouldRejectWhenMinioDisabled() {
        MinioStorageProperties properties = buildEnabledProperties();
        properties.setEnabled(false);
        when(minioClientProvider.getIfAvailable()).thenReturn(minioClient);

        MinioFileStorageService service = new MinioFileStorageService(properties, minioClientProvider);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "design.txt",
                "text/plain",
                "demo-content".getBytes()
        );

        BizException exception = assertThrows(
                BizException.class,
                () -> service.uploadAsset(file, 8L, 9L)
        );

        assertEquals("MinIO 未启用，请先完成对象存储配置", exception.getMessage());
        verifyNoInteractions(minioClient);
    }

    @Test
    void generateDownloadUrl_shouldReturnPresignedUrlForManagedPath() throws Exception {
        when(minioClientProvider.getIfAvailable()).thenReturn(minioClient);
        when(minioClient.getPresignedObjectUrl(any())).thenReturn("https://minio.example.com/presigned/demo");

        MinioFileStorageService service = new MinioFileStorageService(buildEnabledProperties(), minioClientProvider);

        String url = service.generateDownloadUrl("minio://abac/project-assets/8/demo.txt");

        assertEquals("https://minio.example.com/presigned/demo", url);
        verify(minioClient).getPresignedObjectUrl(any());
    }

    @Test
    void generateDownloadUrl_shouldRejectIllegalManagedPath() throws Exception {
        when(minioClientProvider.getIfAvailable()).thenReturn(minioClient);
        MinioFileStorageService service = new MinioFileStorageService(buildEnabledProperties(), minioClientProvider);

        BizException exception = assertThrows(
                BizException.class,
                () -> service.generateDownloadUrl("minio://abac")
        );

        assertEquals("非法的 MinIO 存储路径", exception.getMessage());
        verify(minioClient, never()).getPresignedObjectUrl(any());
    }

    @Test
    void delete_shouldRemoveManagedObject() throws Exception {
        when(minioClientProvider.getIfAvailable()).thenReturn(minioClient);
        doNothing().when(minioClient).removeObject(any());

        MinioFileStorageService service = new MinioFileStorageService(buildEnabledProperties(), minioClientProvider);
        service.delete("minio://abac/project-assets/8/demo.txt");

        verify(minioClient).removeObject(any());
    }

    @Test
    void delete_shouldIgnoreExternalPath() {
        when(minioClientProvider.getIfAvailable()).thenReturn(minioClient);
        MinioFileStorageService service = new MinioFileStorageService(buildEnabledProperties(), minioClientProvider);

        service.delete("https://example.com/demo.txt");

        verifyNoInteractions(minioClient);
    }

    @Test
    void isManagedPath_shouldIdentifyMinioPrefix() {
        when(minioClientProvider.getIfAvailable()).thenReturn(minioClient);
        MinioFileStorageService service = new MinioFileStorageService(buildEnabledProperties(), minioClientProvider);

        assertTrue(service.isManagedPath("minio://abac/project-assets/8/demo.txt"));
        assertNotNull(service);
    }

    private MinioStorageProperties buildEnabledProperties() {
        MinioStorageProperties properties = new MinioStorageProperties();
        properties.setEnabled(true);
        properties.setBucket("abac");
        properties.setDownloadExpiryMinutes(15);
        properties.setAutoCreateBucket(true);
        return properties;
    }
}
