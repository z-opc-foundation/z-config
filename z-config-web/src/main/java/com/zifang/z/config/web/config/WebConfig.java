package com.zifang.z.config.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * Web配置：注册拦截器
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private TokenInterceptor tokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/**") // 拦截所有请求
                .excludePathPatterns( // 排除公开接口（与isPublicUrl逻辑一致，双重保障）
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/logout",
                        "/api/public/**",
                        "/api/config-auth/login",
                        "/api/config-auth/logout",
                        "/auth/**",  // TASK001: 认证相关接口
                        // /api/config/** 和 /api/naming/** 不再排除
                        // 认证由 z.config.auth.enabled 配置开关控制
                        "/swagger-ui/**", // 排除Swagger文档（开发环境）
                        "/v3/api-docs/**",
                        "/doc.html",
                        "/swagger-resources",
                        "/", // 首页
                        "/index.html",
                        "/login.html",
                        "/static/**", // 静态资源
                        "/assets/**", // 前端打包的静态资源
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/**/*.css",
                        "/**/*.js",
                        "/**/*.png",
                        "/**/*.jpg",
                        "/**/*.svg",
                        "/**/*.ico",
                        "/favicon.ico"
                );
    }
}