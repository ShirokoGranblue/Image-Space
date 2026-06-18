package com.picmgmt.image;

public record ImageVariant(
        byte[] bytes,
        String ext,
        String contentType,
        int width,
        int height
) {
}
