package com.xie.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "storage.minio")
public class MinioStorageProperties {
    /**
     * 是否启用 MinIO 存储。
     * 关闭时，项目仍可保留“只保存外部地址”的旧模式。
     */
    private boolean enabled = false;

    /**
     * MinIO 的 S3 API 地址。
     * 这里应连接 9000 端口，而不是浏览器 Console 使用的 9001。
     */
    private String endpoint;

    /**
     * MinIO 登录账号。
     */
    private String accessKey;

    /**
     * MinIO 登录密码。
     */
    private String secretKey;

    /**
     * 默认文件桶名称。
     */
    private String bucket = "abac";

    /**
     * 预签名下载链接过期时间，单位分钟。
     */
    private int downloadExpiryMinutes = 15;

    /**
     * 本地开发环境下，如果 bucket 不存在则自动创建。
     */
    private boolean autoCreateBucket = true;
}
