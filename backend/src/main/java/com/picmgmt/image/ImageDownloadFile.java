package com.picmgmt.image;

public record ImageDownloadFile(
        byte[] bytes,
        String contentType,
        String filename,
        String cacheControl
) {
}
