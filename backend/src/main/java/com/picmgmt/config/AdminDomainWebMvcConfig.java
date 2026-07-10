package com.picmgmt.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class AdminDomainWebMvcConfig implements WebMvcConfigurer {

    private final AdminDomainInterceptor adminDomainInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminDomainInterceptor)
                .addPathPatterns("/admin/**");
    }
}
