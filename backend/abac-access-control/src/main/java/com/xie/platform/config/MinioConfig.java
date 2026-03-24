package com.xie.platform.config;

import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioStorageProperties.class)
public class MinioConfig {

    @Bean
    @ConditionalOnProperty(prefix = "storage.minio", name = "enabled", havingValue = "true")
    public MinioClient minioClient(MinioStorageProperties properties) {
        // 这里只负责构造 MinIO Client，具体 bucket 初始化与上传/下载逻辑交给存储服务处理。
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }
}
