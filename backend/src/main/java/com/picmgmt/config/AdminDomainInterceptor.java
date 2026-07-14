package com.picmgmt.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Locale;

@Component
public class AdminDomainInterceptor implements HandlerInterceptor {

    private static final String ADMIN_HOST = "admin.image-space.app";

    private final boolean allowLocal;

    public AdminDomainInterceptor(@Value("${app.admin.allow-local:false}") boolean allowLocal) {
        this.allowLocal = allowLocal;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String host = normalizeHost(firstNonBlank(request.getHeader("X-Forwarded-Host"), request.getHeader("Host")));
        if (isAllowed(host)) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"message\":\"当前域名无权访问后台管理系统\",\"data\":null}");
        return false;
    }

    private boolean isAllowed(String host) {
        if (ADMIN_HOST.equals(host)) {
            return true;
        }
        return allowLocal && ("localhost".equals(host) || "127.0.0.1".equals(host));
    }

    private String normalizeHost(String host) {
        if (host == null || host.isBlank()) {
            return "";
        }
        String first = host.split(",")[0].trim().toLowerCase(Locale.ROOT);
        if (first.startsWith("[")) {
            int end = first.indexOf(']');
            return end >= 0 ? first.substring(1, end) : first;
        }
        int portIndex = first.indexOf(':');
        return portIndex >= 0 ? first.substring(0, portIndex) : first;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
