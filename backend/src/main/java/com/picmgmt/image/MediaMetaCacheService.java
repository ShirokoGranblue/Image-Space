package com.picmgmt.image;

import com.picmgmt.config.MediaMetaCacheProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaMetaCacheService {

    private final MediaMetaCacheProperties properties;
    private final RestClient restClient = RestClient.create();

    private static final String META_KEY_PREFIX = "media:meta:";

    public void evict(String storageKey) {
        String normalizedKey = normalizeKey(storageKey);
        if (normalizedKey.isBlank()) {
            return;
        }
        if (!configured()) {
            log.debug("Upstash media meta cache is not configured; skip evict for {}", normalizedKey);
            return;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(properties.getUpstashUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.getUpstashToken())
                    .body(List.of("DEL", META_KEY_PREFIX + normalizedKey))
                    .retrieve()
                    .body(Map.class);
            if (response != null && response.get("error") != null) {
                log.warn("Upstash media meta eviction failed for {}: {}", normalizedKey, response.get("error"));
            }
        } catch (Exception e) {
            log.warn("Upstash media meta eviction failed for {}: {}", normalizedKey, e.getMessage());
        }
    }

    private boolean configured() {
        return properties.getUpstashUrl() != null && !properties.getUpstashUrl().isBlank()
                && properties.getUpstashToken() != null && !properties.getUpstashToken().isBlank();
    }

    private String normalizeKey(String storageKey) {
        return storageKey == null ? "" : storageKey.replaceAll("^/+", "");
    }
}
