package com.picmgmt.image;

import com.picmgmt.cache.CacheService;
import com.picmgmt.util.MediaUrlUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageUrlServiceTest {

    @Mock private CacheService cacheService;
    @Mock private MediaUrlUtil mediaUrlUtil;

    @Test
    void getPrivateImageUrl_shouldIssueBackendAuthorizedWorkerUrl() {
        when(cacheService.get("media:url:images/a.png", String.class)).thenReturn(Optional.empty());
        when(cacheService.get("media:token:index:images/a.png", String.class)).thenReturn(Optional.empty());
        when(mediaUrlUtil.getPublicUrl()).thenReturn("https://cdn.image-space.app");
        ImageUrlService service = new ImageUrlService(cacheService, mediaUrlUtil);

        String url = service.getPrivateImageUrl("/images/a.png");

        assertTrue(url.startsWith("https://cdn.image-space.app/private/images/a.png?auth="));
        assertTrue(url.contains("&expires="));
        assertFalse(url.contains("&v="));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(cacheService).put(eq("media:url:images/a.png"), valueCaptor.capture(), eq(ImageUrlService.PRIVATE_ACCESS_EXPIRY.minusSeconds(5)));
        assertEquals(url, valueCaptor.getValue());
        verify(cacheService).put(eq("media:token:index:images/a.png"), any(String.class), eq(ImageUrlService.PRIVATE_ACCESS_EXPIRY));
    }

    @Test
    void authorizePrivateAccess_shouldAcceptMatchingUnexpiredToken() {
        long expiresAt = Instant.now().plusSeconds(30).getEpochSecond();
        when(cacheService.get("media:token:abc", String.class))
                .thenReturn(Optional.of("images/a.png\n" + expiresAt));
        ImageUrlService service = new ImageUrlService(cacheService, mediaUrlUtil);

        assertTrue(service.authorizePrivateAccess("images/a.png", "abc"));
    }

    @Test
    void authorizePrivateAccess_shouldRejectWrongKey() {
        long expiresAt = Instant.now().plusSeconds(30).getEpochSecond();
        when(cacheService.get("media:token:abc", String.class))
                .thenReturn(Optional.of("images/a.png\n" + expiresAt));
        ImageUrlService service = new ImageUrlService(cacheService, mediaUrlUtil);

        assertFalse(service.authorizePrivateAccess("images/other.png", "abc"));
    }

    @Test
    void authorizePrivateAccess_shouldRejectExpiredToken() {
        long expiresAt = Instant.now().minusSeconds(1).getEpochSecond();
        when(cacheService.get("media:token:abc", String.class))
                .thenReturn(Optional.of("images/a.png\n" + expiresAt));
        ImageUrlService service = new ImageUrlService(cacheService, mediaUrlUtil);

        assertFalse(service.authorizePrivateAccess("images/a.png", "abc"));
        verify(cacheService).evict("media:token:abc");
    }
}
