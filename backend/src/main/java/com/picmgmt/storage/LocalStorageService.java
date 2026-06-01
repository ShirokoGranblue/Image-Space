package com.picmgmt.storage;

import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local")
public class LocalStorageService implements StorageService {

    @Value("${storage.local.base-path:./storage}")
    private String basePath;

    @Override
    public String upload(String bucket, String objectKey, byte[] bytes, String contentType) {
        return upload(bucket, objectKey, bytes, contentType, null);
    }

    @Override
    public String upload(String bucket, String objectKey, byte[] bytes, String contentType, String cacheControl) {
        try {
            Path targetPath = Paths.get(basePath, bucket, objectKey);
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, bytes);
            return "/storage/" + bucket + "/" + objectKey;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_UPLOAD_FAILED, e);
        }
    }

    @Override
    public byte[] download(String bucket, String objectKey) {
        try {
            Path targetPath = Paths.get(basePath, bucket, objectKey);
            return Files.readAllBytes(targetPath);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_DOWNLOAD_FAILED, e);
        }
    }

    @Override
    public void delete(String bucket, String objectKey) {
        try {
            Files.deleteIfExists(Paths.get(basePath, bucket, objectKey));
        } catch (IOException e) {
            log.warn("本地文件删除失败: {}/{}", bucket, objectKey);
        }
    }

    @Override
    public String getAccessUrl(String bucket, String objectKey) {
        return "/api/files/" + bucket + "/" + objectKey;
    }

    @Override
    public String getPresignedUrl(String bucket, String objectKey, java.time.Duration expiry) {
        return "/api/files/" + bucket + "/" + objectKey;
    }

    @Override
    public FileMeta getFileMeta(String bucket, String objectKey) {
        try {
            Path path = Paths.get(basePath, bucket, objectKey);
            long size = Files.size(path);
            String ct = Files.probeContentType(path);
            return new FileMeta(size, ct != null ? ct : "application/octet-stream");
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void updateObjectMetadata(String bucket, String objectKey, String cacheControl, boolean isPublic) {
        // 本地存储不支持自定义元数据，忽略
    }
}
