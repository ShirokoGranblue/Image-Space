package com.picmgmt.service.impl;

import cn.hutool.core.codec.Base64;
import com.picmgmt.config.GoogleJwtVerifier;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoogleOAuthEmailVerificationTest {

    @Test
    void idTokenDoesNotExposeUnverifiedEmailToAccountLinking() {
        AuthUser user = userFromClaims(false);

        assertNull(user.getEmail());
    }

    @Test
    void idTokenKeepsVerifiedEmail() {
        AuthUser user = userFromClaims(true);

        assertEquals("user@example.com", user.getEmail());
    }

    private AuthUser userFromClaims(boolean emailVerified) {
        GoogleJwtVerifier verifier = mock(GoogleJwtVerifier.class);
        String payload = "{\"sub\":\"subject\",\"email\":\"user@example.com\","
                + "\"email_verified\":" + emailVerified + ",\"name\":\"User\"}";
        String idToken = "e30." + Base64.encodeUrlSafe(payload) + ".signature";
        when(verifier.verify(idToken, "client-id")).thenReturn(true);
        OAuthServiceImpl.IdTokenGoogleRequest request = new OAuthServiceImpl.IdTokenGoogleRequest(
                AuthConfig.builder()
                        .clientId("client-id")
                        .clientSecret("client-secret")
                        .redirectUri("https://image-space.app/callback")
                        .build(),
                verifier
        );

        return request.getUserInfo(AuthToken.builder().idToken(idToken).build());
    }
}
