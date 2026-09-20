package com.picmgmt.storage;

import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
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
            Path targetPath = resolveForWrite(bucket, objectKey);
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
            Path targetPath = resolveExisting(bucket, objectKey);
            return Files.readAllBytes(targetPath);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_DOWNLOAD_FAILED, e);
        }
    }

    @Override
    public void delete(String bucket, String objectKey) {
        try {
            Files.deleteIfExists(resolveExisting(bucket, objectKey));
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.warn("本地文件删除失败: {}/{}", bucket, objectKey);
        }
    }

    @Override
    public String getAccessUrl(String bucket, String objectKey) {
        return "/api/files/" + bucket + "/" + objectKey;
    }

    @Override
    public void deleteObjectReliably(String bucket, String objectKey) {
        try {
            Files.deleteIfExists(resolveExisting(bucket, objectKey));
        } catch (IOException e) {
            throw new IllegalStateException("Object cleanup failed", e);
        }
    }

    @Override
    public String getPresignedUrl(String bucket, String objectKey, java.time.Duration expiry) {
        return "/api/files/" + bucket + "/" + objectKey;
    }

    @Override
    public FileMeta getFileMeta(String bucket, String objectKey) {
        try {
            Path path = resolveExisting(bucket, objectKey);
            long size = Files.size(path);
            String ct = Files.probeContentType(path);
            return new FileMeta(size, ct != null ? ct : "application/octet-stream");
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void updateObjectMetadata(String bucket, String objectKey, String cacheControl, boolean isPublic) {
        // 本地存储不支持自定义元数据，忽略
    }

    private Path resolveForWrite(String bucket, String objectKey) throws IOException {
        Path bucketRoot = bucketRoot(bucket);
        Files.createDirectories(bucketRoot);
        Path target = lexicalTarget(bucketRoot, objectKey);
        Files.createDirectories(target.getParent());
        Path realBucketRoot = bucketRoot.toRealPath();
        Path realParent = target.getParent().toRealPath();
        if (!realParent.startsWith(realBucketRoot)
                || Files.isSymbolicLink(target)
                || Files.exists(target, LinkOption.NOFOLLOW_LINKS) && !target.toRealPath().startsWith(realBucketRoot)) {
            throw invalidPath();
        }
        return target;
    }

    private Path resolveExisting(String bucket, String objectKey) throws IOException {
        Path bucketRoot = bucketRoot(bucket);
        Path target = lexicalTarget(bucketRoot, objectKey);
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            return target;
        }
        Path realBucketRoot = bucketRoot.toRealPath();
        Path realTarget = target.toRealPath();
        if (!realTarget.startsWith(realBucketRoot)) {
            throw invalidPath();
        }
        return realTarget;
    }

    private Path bucketRoot(String bucket) {
        if (bucket == null || !bucket.matches("[A-Za-z0-9_-]+")) {
            throw invalidPath();
        }
        Path baseRoot = Paths.get(basePath).toAbsolutePath().normalize();
        Path bucketRoot = baseRoot.resolve(bucket).normalize();
        if (!bucketRoot.startsWith(baseRoot)) {
            throw invalidPath();
        }
        return bucketRoot;
    }

    private Path lexicalTarget(Path bucketRoot, String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw invalidPath();
        }
        Path target = bucketRoot.resolve(objectKey).normalize();
        if (!target.startsWith(bucketRoot)) {
            throw invalidPath();
        }
        return target;
    }

    private BusinessException invalidPath() {
        return new BusinessException(ErrorCode.BAD_REQUEST, "非法存储路径");
    }
}
