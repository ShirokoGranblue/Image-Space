package com.picmgmt.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 图片缓存服务 — 提供 Base64 ↔ byte[] 互转的内存缓存。
 * <p>
 * 使用 LRU 策略的 ConcurrentLinkedHashMap 缓存已解码的 byte[]，
 * 用户操作优先命中缓存，未命中时再从 Base64 字符串解码并写入缓冲区。
 * </p>
 */
@Slf4j
@Service
public class ImageCacheService {

    /** 缓存最大条目数（约 200 张图片） */
    private static final int MAX_CACHE_SIZE = 200;

    /** LRU 缓存: cacheKey -> decoded byte[] */
    private final Map<String, byte[]> cache;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ImageCacheService() {
        // accessOrder=true 使得 LinkedHashMap 按访问顺序排列（LRU）
        this.cache = new LinkedHashMap<>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        };
    }

    // ===================== 编码：byte[] → Base64 Data URL =====================

    /**
     * 将图片字节数组编码为 Base64 Data URL，并写入缓存。
     *
     * @param cacheKey  缓存键（如 "image:123" 或 "avatar:456"）
     * @param bytes     原始图片字节
     * @param mediaType MIME 类型（如 "image/jpeg"）
     * @return 完整的 Data URL，如 "data:image/jpeg;base64,/9j/4AAQ..."
     */
    public String encodeToDataUrl(String cacheKey, byte[] bytes, String mediaType) {
        // 先写入缓存
        putCache(cacheKey, bytes);
        // 编码为 Base64 Data URL
        String base64 = Base64.getEncoder().encodeToString(bytes);
        return "data:" + mediaType + ";base64," + base64;
    }

    // ===================== 解码：Base64 Data URL → byte[] =====================

    /**
     * 从缓存或 Base64 Data URL 获取图片字节数组。
     * 优先命中缓存，未命中则解码并写入缓冲区。
     *
     * @param cacheKey 缓存键
     * @param dataUrl  Base64 Data URL
     * @return 解码后的图片字节数组
     */
    public byte[] decodeFromDataUrl(String cacheKey, String dataUrl) {
        // 优先命中缓存
        byte[] cached = getCache(cacheKey);
        if (cached != null) {
            log.debug("缓存命中: {}", cacheKey);
            return cached;
        }

        // 未命中，解码 Base64
        log.debug("缓存未命中，解码 Base64: {}", cacheKey);
        String base64Data = extractBase64Data(dataUrl);
        byte[] decoded = Base64.getDecoder().decode(base64Data);

        // 写入缓冲区
        putCache(cacheKey, decoded);
        return decoded;
    }

    // ===================== 缓存操作 =====================

    /**
     * 从缓存获取（读锁）
     */
    private byte[] getCache(String key) {
        lock.readLock().lock();
        try {
            return cache.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 写入缓存（写锁）
     */
    private void putCache(String key, byte[] data) {
        lock.writeLock().lock();
        try {
            cache.put(key, data);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 移除缓存条目
     */
    public void evict(String cacheKey) {
        lock.writeLock().lock();
        try {
            cache.remove(cacheKey);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 清空全部缓存
     */
    public void clearAll() {
        lock.writeLock().lock();
        try {
            cache.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ===================== 工具方法 =====================

    /**
     * 从 Data URL 中提取纯 Base64 字符串部分。
     * 输入: "data:image/jpeg;base64,/9j/4AAQ..."
     * 输出: "/9j/4AAQ..."
     */
    public static String extractBase64Data(String dataUrl) {
        if (dataUrl == null) return "";
        int commaIndex = dataUrl.indexOf(',');
        if (commaIndex < 0) return dataUrl;
        return dataUrl.substring(commaIndex + 1);
    }

    /**
     * 从 Data URL 中提取 MIME 类型。
     * 输入: "data:image/jpeg;base64,..."
     * 输出: "image/jpeg"
     */
    public static String extractMediaType(String dataUrl) {
        if (dataUrl == null || !dataUrl.startsWith("data:")) return "application/octet-stream";
        int colonIndex = dataUrl.indexOf(':');
        int semicolonIndex = dataUrl.indexOf(';');
        if (colonIndex < 0 || semicolonIndex < 0) return "application/octet-stream";
        return dataUrl.substring(colonIndex + 1, semicolonIndex);
    }

    /**
     * 根据文件扩展名获取 MIME 类型
     */
    public static String getMimeType(String ext) {
        return switch (ext.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    /**
     * 判断字符串是否为 Data URL
     */
    public static boolean isDataUrl(String s) {
        return s != null && s.startsWith("data:");
    }
}
