package com.picmgmt.storage;

public interface StorageService {

    String upload(String bucket, String objectKey, byte[] bytes, String contentType);

    byte[] download(String bucket, String objectKey);

    void delete(String bucket, String objectKey);

    String getAccessUrl(String bucket, String objectKey);

    FileMeta getFileMeta(String bucket, String objectKey);

    record FileMeta(long size, String contentType) {}
}
