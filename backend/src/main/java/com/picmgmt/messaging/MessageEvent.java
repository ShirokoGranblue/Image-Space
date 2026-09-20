package com.picmgmt.messaging;

import com.picmgmt.entity.AuditLog;
import java.util.List;

public record MessageEvent(String eventId, int version, String type, String imageUuid,
                           String originalKey, AuditLog audit, List<String> cleanupKeys) {
    public static final String IMAGE = "IMAGE_VARIANTS";
    public static final String AUDIT = "AUDIT";
    public static final String CLEANUP = "IMAGE_CLEANUP";
    public static final String CACHE = "IMAGE_CACHE";
}
