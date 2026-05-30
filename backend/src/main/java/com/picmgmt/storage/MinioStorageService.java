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
        if (objectKey == null || objectKey.isBlank()) {
            throw new BusinessException(ErrorCode.STORAGE_UPLOAD_FAILED);
        }

        objectKey = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;

        String finalContentType = contentType != null && !contentType.isBlank()
                ? contentType
                : "application/octet-stream";

        try (ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName())
                            .object(objectKey)
                            .stream(is, bytes.length, -1)
                            .contentType(finalContentType)
                            .build());

            log.debug("上传成功: {}/{} ({} bytes)", bucketName(), objectKey, bytes.length);
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
}
