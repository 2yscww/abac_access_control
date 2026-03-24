package com.xie.platform.service.impl;

import com.xie.platform.config.MinioStorageProperties;
import com.xie.platform.exception.BizException;
import com.xie.platform.service.FileStorageService;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class MinioFileStorageService implements FileStorageService {

    private static final String STORAGE_PREFIX = "minio://";

    private final MinioStorageProperties properties;
    private final MinioClient minioClient;
    private final AtomicBoolean bucketReady = new AtomicBoolean(false);

    public MinioFileStorageService(
            MinioStorageProperties properties,
            ObjectProvider<MinioClient> minioClientProvider) {
        this.properties = properties;
        this.minioClient = minioClientProvider.getIfAvailable();
    }

    @Override
    public String uploadAsset(MultipartFile file, Long projectId, Long employeeId) {
        ensureEnabled();
        ensureBucketReady();

        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件不能为空");
        }

        String objectName = buildObjectName(projectId, employeeId, file.getOriginalFilename());
        String contentType = StringUtils.hasText(file.getContentType())
                ? file.getContentType()
                : "application/octet-stream";

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );
            return STORAGE_PREFIX + properties.getBucket() + "/" + objectName;
        } catch (Exception exception) {
            throw new BizException("文件上传到 MinIO 失败");
        }
    }

    @Override
    public String generateDownloadUrl(String storagePath) {
        ensureEnabled();

        MinioLocation location = parseLocation(storagePath);
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(location.bucket())
                            .object(location.objectKey())
                            .expiry(properties.getDownloadExpiryMinutes(), TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception exception) {
            throw new BizException("生成文件下载地址失败");
        }
    }

    @Override
    public void delete(String storagePath) {
        if (!isManagedPath(storagePath)) {
            return;
        }

        ensureEnabled();

        MinioLocation location = parseLocation(storagePath);
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(location.bucket())
                            .object(location.objectKey())
                            .build()
            );
        } catch (Exception exception) {
            throw new BizException("删除 MinIO 文件失败");
        }
    }

    @Override
    public boolean isManagedPath(String storagePath) {
        return StringUtils.hasText(storagePath) && storagePath.startsWith(STORAGE_PREFIX);
    }

    private void ensureEnabled() {
        if (!properties.isEnabled() || minioClient == null) {
            throw new BizException("MinIO 未启用，请先完成对象存储配置");
        }
    }

    private void ensureBucketReady() {
        if (bucketReady.get()) {
            return;
        }

        synchronized (bucketReady) {
            if (bucketReady.get()) {
                return;
            }

            try {
                boolean exists = minioClient.bucketExists(
                        BucketExistsArgs.builder()
                                .bucket(properties.getBucket())
                                .build()
                );
                if (!exists) {
                    if (!properties.isAutoCreateBucket()) {
                        throw new BizException("MinIO Bucket 不存在，请先手动创建: " + properties.getBucket());
                    }
                    minioClient.makeBucket(
                            MakeBucketArgs.builder()
                                    .bucket(properties.getBucket())
                                    .build()
                    );
                }
                bucketReady.set(true);
            } catch (BizException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new BizException("初始化 MinIO Bucket 失败");
            }
        }
    }

    private String buildObjectName(Long projectId, Long employeeId, String originalFilename) {
        LocalDate now = LocalDate.now();
        String filename = sanitizeFilename(originalFilename);
        return "project-assets/%s/%s/%s/%s/%s-%s".formatted(
                projectId,
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth(),
                employeeId,
                UUID.randomUUID() + "-" + filename
        );
    }

    private String sanitizeFilename(String originalFilename) {
        String filename = StringUtils.getFilename(
                StringUtils.hasText(originalFilename) ? originalFilename : "file.bin"
        );
        String sanitized = filename == null ? "file.bin" : filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return StringUtils.hasText(sanitized) ? sanitized : "file.bin";
    }

    private MinioLocation parseLocation(String storagePath) {
        if (!isManagedPath(storagePath)) {
            throw new BizException("非法的 MinIO 存储路径");
        }

        String rawPath = storagePath.substring(STORAGE_PREFIX.length());
        int firstSlash = rawPath.indexOf('/');
        if (firstSlash <= 0 || firstSlash == rawPath.length() - 1) {
            throw new BizException("非法的 MinIO 存储路径");
        }

        String bucket = rawPath.substring(0, firstSlash);
        String objectKey = rawPath.substring(firstSlash + 1);
        return new MinioLocation(bucket, objectKey);
    }

    private record MinioLocation(String bucket, String objectKey) {
    }
}
