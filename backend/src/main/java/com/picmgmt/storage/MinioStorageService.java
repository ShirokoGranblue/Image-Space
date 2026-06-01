package com.picmgmt.storage;

import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.config.MinioConfig;
import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "r2", matchIfMissing = true)
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    public MinioStorageService(MinioClient minioClient, MinioConfig minioConfig) {
        this.minioClient = minioClient;
        this.minioConfig = minioConfig;
    }

    private String bucketName() {
        return minioConfig.getBucketName();
    }

    private String cdnHost() {
        String url = minioConfig.getPublicUrl();
        if (url == null || url.isBlank()) return null;
        return url.replaceFirst("https?://", "").replaceAll("/$", "");
    }

    @Override
    public String upload(String bucket, String objectKey, byte[] bytes, String contentType) {
        return upload(bucket, objectKey, bytes, contentType, "no-store, must-revalidate");
    }

    @Override
    public String upload(String bucket, String objectKey, byte[] bytes, String contentType, String cacheControl) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new BusinessException(ErrorCode.STORAGE_UPLOAD_FAILED);
        }

        objectKey = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;

        String finalContentType = contentType != null && !contentType.isBlank()
                ? contentType
                : "application/octet-stream";

        String cc = cacheControl != null ? cacheControl : "no-store, must-revalidate";

        try (ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
            var headerMap = new java.util.HashMap<String, String>();
            headerMap.put("Cache-Control", cc);
            // 公开图片设置 x-amz-meta-public，Worker 据此允许无签名访问
            if (cc != null && cc.startsWith("public")) {
                headerMap.put("x-amz-meta-public", "true");
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName())
                            .object(objectKey)
                            .stream(is, bytes.length, -1)
                            .contentType(finalContentType)
                            .headers(headerMap)
                            .build());

            log.debug("上传成功: {}/{} ({} bytes, Cache-Control: {})", bucketName(), objectKey, bytes.length, cc);
            return objectKey;
        } catch (Exception e) {
            log.error("上传失败: bucket={}, objectKey={}, size={}", bucketName(), objectKey, bytes.length, e);
            throw new BusinessException(ErrorCode.STORAGE_UPLOAD_FAILED, e);
        }
    }

    @Override
    public byte[] download(String bucket, String objectKey) {
        try (var is = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucketName()).object(objectKey).build())) {
            return is.readAllBytes();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.STORAGE_DOWNLOAD_FAILED, e);
        }
    }

    @Override
    public void delete(String bucket, String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName()).object(objectKey).build());
        } catch (Exception e) {
            log.warn("删除文件失败: {}/{}", bucketName(), objectKey);
        }
    }

    @Override
    public String getAccessUrl(String bucket, String objectKey) {
        return getPresignedUrl(bucket, objectKey, java.time.Duration.ofHours(1));
    }

    @Override
    public String getPresignedUrl(String bucket, String objectKey, java.time.Duration expiry) {
        try {
            String presignedUrl = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName()).object(objectKey)
                    .method(io.minio.http.Method.GET)
                    .expiry((int) expiry.getSeconds(), TimeUnit.SECONDS)
                    .build());

            String cdn = cdnHost();
            if (cdn != null) {
                try {
                    URI uri = new URI(presignedUrl);
                    String path = uri.getRawPath();
                    String bucketPrefix = "/" + bucketName();
                    if (path.startsWith(bucketPrefix + "/")) {
                        path = path.substring(bucketPrefix.length());
                    }
                    presignedUrl = "https://" + cdn + path + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "");
                } catch (Exception e) {
                    log.warn("URL rewrite failed, using original: {}", e.getMessage());
                }
            }
            return presignedUrl;
        } catch (Exception e) {
            log.warn("生成预签名 URL 失败: {}/{}", bucketName(), objectKey);
            return null;
        }
    }

    @Override
    public FileMeta getFileMeta(String bucket, String objectKey) {
        try {
            var stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName()).object(objectKey).build());
            return new FileMeta(stat.size(), stat.contentType());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void updateObjectMetadata(String bucket, String objectKey, String cacheControl, boolean isPublic) {
        try {
            var headerMap = new java.util.HashMap<String, String>();
            headerMap.put("Cache-Control", cacheControl);
            headerMap.put("x-amz-meta-public", isPublic ? "true" : "false");
            headerMap.put("x-amz-metadata-directive", "REPLACE");

            // R2/S3 不支持直接修改元数据，通过 copy-in-place + REPLACE 实现
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName())
                            .object(objectKey)
                            .source(CopySource.builder()
                                    .bucket(bucketName())
                                    .object(objectKey)
                                    .build())
                            .headers(headerMap)
                            .build());
            log.debug("元数据已更新: {}/{} → Cache-Control={}, public={}", bucketName(), objectKey, cacheControl, isPublic);
        } catch (Exception e) {
            log.error("Failed to update metadata: {}/{}", bucketName(), objectKey, e);
            throw new BusinessException(ErrorCode.STORAGE_UPLOAD_FAILED, e);
        }
    }
}
