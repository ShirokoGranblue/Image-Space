package com.picmgmt.storage;

import java.time.Duration;

public interface StorageService {

    String upload(String bucket, String objectKey, byte[] bytes, String contentType);

    String upload(String bucket, String objectKey, byte[] bytes, String contentType, String cacheControl);

    byte[] download(String bucket, String objectKey);

    void delete(String bucket, String objectKey);

    String getAccessUrl(String bucket, String objectKey);

    String getPresignedUrl(String bucket, String objectKey, Duration expiry);

    FileMeta getFileMeta(String bucket, String objectKey);

    /**
     * 更新已有对象的元数据（通过 copy-in-place 实现）。
     * 用于权限变更时同步 R2 Cache-Control 和 x-amz-meta-public。
     */
    void updateObjectMetadata(String bucket, String objectKey, String cacheControl, boolean isPublic);

    record FileMeta(long size, String contentType) {}
}
