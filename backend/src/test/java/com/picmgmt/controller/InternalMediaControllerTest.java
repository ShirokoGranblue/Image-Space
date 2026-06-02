package com.picmgmt.controller;

import com.picmgmt.config.InternalMediaProperties;
import com.picmgmt.image.InternalMediaService;
import com.picmgmt.vo.MediaMetaVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalMediaControllerTest {

    @Test
    void meta_shouldRejectMissingInternalToken() {
        InternalMediaService service = mock(InternalMediaService.class);
        InternalMediaController controller = new InternalMediaController(properties(), service);

        var response = controller.meta("images/a.png", null);

        assertEquals(403, response.getStatusCode().value());
        verify(service, never()).getMeta("images/a.png");
    }

    @Test
    void meta_shouldReturnNoStoreMediaMetaWhenTokenMatches() {
        InternalMediaService service = mock(InternalMediaService.class);
        when(service.getMeta("images/a.png")).thenReturn(new MediaMetaVO(7L, "images/a.png", "public", 4L, 2L));
        InternalMediaController controller = new InternalMediaController(properties(), service);

        var response = controller.meta("images/a.png", "secret");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals(2L, response.getBody().getVersion());
    }

    @Test
    void authorize_shouldReturnNoContentWhenAllowed() {
        InternalMediaService service = mock(InternalMediaService.class);
        when(service.authorize("images/a.png", "token", null, null)).thenReturn(true);
        InternalMediaController controller = new InternalMediaController(properties(), service);

        var response = controller.authorize("images/a.png", "token", null, null, "secret");

        assertEquals(204, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getCacheControl());
    }

    private InternalMediaProperties properties() {
        InternalMediaProperties properties = new InternalMediaProperties();
        properties.setToken("secret");
        return properties;
    }
}
