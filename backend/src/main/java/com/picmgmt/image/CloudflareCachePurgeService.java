package com.picmgmt.image;

import com.picmgmt.config.CloudflareProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudflareCachePurgeService {

    private final CloudflareProperties properties;
    private final RestClient restClient = RestClient.create();

    public void purgeFiles(Collection<String> urls) {
        List<String> files = urls == null
                ? List.of()
                : urls.stream()
                    .filter(url -> url != null && !url.isBlank())
                    .distinct()
                    .toList();
        if (files.isEmpty()) {
            return;
        }
        if (!configured()) {
            log.warn("Cloudflare purge is not configured; skip purge for {} URL(s)", files.size());
            return;
        }

        try {
            String purgeEndpoint = properties.getPurgeUrl().formatted(properties.getZoneId());
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(purgeEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.getApiToken())
                    .body(Map.of("files", files))
                    .retrieve()
                    .body(Map.class);
            if (response != null && Boolean.FALSE.equals(response.get("success"))) {
                log.warn("Cloudflare purge returned failure for {} URL(s): {}", files.size(), response.get("errors"));
            } else {
                log.info("Cloudflare purge requested for {} URL(s)", files.size());
            }
        } catch (Exception e) {
            log.warn("Cloudflare purge failed for {} URL(s): {}", files.size(), e.getMessage());
        }
    }

    public void purgeFile(String url) {
        purgeFiles(List.of(url));
    }

    private boolean configured() {
        return properties.getZoneId() != null && !properties.getZoneId().isBlank()
                && properties.getApiToken() != null && !properties.getApiToken().isBlank()
                && properties.getPurgeUrl() != null && !properties.getPurgeUrl().isBlank();
    }
}
