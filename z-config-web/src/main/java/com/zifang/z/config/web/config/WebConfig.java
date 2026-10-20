package com.zifang.z.config.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * Web配置：注册拦截器 + SPA history fallback
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private TokenInterceptor tokenInterceptor;

    /**
     * SPA history fallback: 前端 BrowserRouter 刷新 /config 等子路由时 Spring 找不到静态资源
     * 就返回 JSON 404, 浏览器拿不到 index.html 直接白屏。vite dev/preview 自带 fallback,
     * 本地测不出来, 只在生产现形。api/ 与 assets/ 前缀保持原样不回落。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected org.springframework.core.io.Resource getResource(
                            String resourcePath, org.springframework.core.io.Resource location) throws IOException {
                        org.springframework.core.io.Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        if (resourcePath.startsWith("api/") || resourcePath.startsWith("assets/")) {
                            return null;
                        }
                        // 任何带扩展名的都不是前端路由：vite base 配错时 index.html 会去请求
                        // /meta/assets/x.js，回 index.html 会得到一个 200 text/html 的假成功，
                        // 浏览器报的是 "Failed to fetch dynamically imported module"，看不出是 base 错了。
                        if (resourcePath.lastIndexOf('.') > resourcePath.lastIndexOf('/')) {
                            return null;
                        }
                        return location.createRelative("index.html");
                    }
                });
    }

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
                        // Nacos 兼容面走 nacos-client 自己的 accessToken 机制，与本服务 Cookie token 无关，
                        // 沿用 TokenInterceptor 会把标准 nacos-client 全部挡在门外，等于兼容面失效
                        "/nacos/**",
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