package com.picmgmt.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MediaUrlUtilTest {

    @Test
    void userAvatarUrl_shouldUseBackendEndpointWithStorageKeyVersion() {
        MediaUrlUtil util = new MediaUrlUtil();

        assertEquals(
                "/api/user/avatar/user-uuid?v=867e55914ef4",
                util.userAvatarUrl("user-uuid", "4/avatar.png")
        );
    }

    @Test
    void userBackgroundUrl_shouldUseBackendEndpointWithStorageKeyVersion() {
        MediaUrlUtil util = new MediaUrlUtil();

        assertEquals(
                "/api/user/background/user-uuid?v=326f74a30ecc",
                util.userBackgroundUrl("user-uuid", "4/background.jpg")
        );
    }
}
