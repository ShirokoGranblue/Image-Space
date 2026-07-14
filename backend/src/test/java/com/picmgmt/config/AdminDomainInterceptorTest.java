package com.picmgmt.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminDomainInterceptorTest {

    @Test
    void productionAllowsOnlyAdminDomain() throws Exception {
        AdminDomainInterceptor interceptor = new AdminDomainInterceptor(false);

        assertTrue(interceptor.preHandle(request("admin.image-space.app"), response(), new Object()));

        MockHttpServletResponse rejected = response();
        assertFalse(interceptor.preHandle(request("image-space.app"), rejected, new Object()));
        assertEquals(403, rejected.getStatus());
    }

    @Test
    void localHostsRequireExplicitOptIn() throws Exception {
        assertFalse(new AdminDomainInterceptor(false)
                .preHandle(request("127.0.0.1:8088"), response(), new Object()));
        assertTrue(new AdminDomainInterceptor(true)
                .preHandle(request("127.0.0.1:8088"), response(), new Object()));
    }

    @Test
    void forwardedHostTakesPrecedence() throws Exception {
        MockHttpServletRequest request = request("backend:8088");
        request.addHeader("X-Forwarded-Host", "admin.image-space.app");

        assertTrue(new AdminDomainInterceptor(false).preHandle(request, response(), new Object()));
    }

    private MockHttpServletRequest request(String host) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Host", host);
        return request;
    }

    private MockHttpServletResponse response() {
        return new MockHttpServletResponse();
    }
}
