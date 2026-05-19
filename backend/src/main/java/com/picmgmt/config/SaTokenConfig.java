package com.picmgmt.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
                    SaRouter.match("/**")
                            .notMatch("/user/login", "/user/register",
                                    "/doc.html", "/v3/api-docs/**", "/swagger-ui/**",
                                    "/image/square", "/user/profile/**",
                                    "/user/check-field",
                                    "/user/send-code", "/user/login-by-code",
                                    "/user/captcha",
                                    "/user/oauth/github", "/user/oauth/github/callback",
                                    "/user/oauth/google", "/user/oauth/google/callback",
                                    "/comment/list/**")
                            .check(r -> StpUtil.checkLogin());
                }))
                .addPathPatterns("/**");
    }
}
