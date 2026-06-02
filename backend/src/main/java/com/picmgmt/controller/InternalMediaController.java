package com.picmgmt.controller;

import com.picmgmt.config.InternalMediaProperties;
import com.picmgmt.image.InternalMediaService;
import com.picmgmt.vo.MediaMetaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/internal/media")
@RequiredArgsConstructor
public class InternalMediaController {

    private final InternalMediaProperties properties;
    private final InternalMediaService internalMediaService;

    @GetMapping("/meta")
    public ResponseEntity<MediaMetaVO> meta(@RequestParam("storageKey") String storageKey,
                                            @RequestHeader(name = "X-Internal-Token", required = false) String internalToken) {
        if (!authorized(internalToken)) {
            log.warn("Rejected internal media meta request without a valid internal token");
            return ResponseEntity.status(403).cacheControl(CacheControl.noStore()).build();
        }
        MediaMetaVO meta = internalMediaService.getMeta(storageKey);
        if (meta == null) {
            return ResponseEntity.status(404).cacheControl(CacheControl.noStore()).build();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(meta);
    }

    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(@RequestParam("storageKey") String storageKey,
                                          @RequestParam(name = "token", required = false) String accessToken,
                                          @RequestHeader(name = "Authorization", required = false) String authorization,
                                          @RequestHeader(name = "satoken", required = false) String saToken,
                                          @RequestHeader(name = "X-Internal-Token", required = false) String internalToken) {
        if (!authorized(internalToken)) {
            log.warn("Rejected internal media authorize request without a valid internal token");
            return ResponseEntity.status(403).cacheControl(CacheControl.noStore()).build();
        }
        boolean allowed = internalMediaService.authorize(storageKey, accessToken, authorization, saToken);
        if (!allowed) {
            return ResponseEntity.status(403).cacheControl(CacheControl.noStore()).build();
        }
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    private boolean authorized(String internalToken) {
        String expected = properties.getToken();
        return expected != null && !expected.isBlank()
                && internalToken != null && expected.equals(internalToken);
    }
}
