package com.zifang.z.config.web.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import jakarta.annotation.Resource;
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
     *
     * <p><b>2026-10-22 (TASK-20261022-T1 · Nacos 兼容面下线)</b>: <b>nacos/</b> 前缀也**不回落**。
     * <p>原因: 兼容面 {@code /nacos/v1/**} 的控制器已整个删除(见 NacosCompatibleController 的删除记录),
     * 没有 handler 时请求会落到本资源处理器 ⇒ 若这里仍然回落 index.html, 兼容面就会以
     * <b>HTTP 200 + 一张 SPA 首页</b> 的形态"看起来还在" ⇒ 那叫**假装下线**, 不是下线。
     * <p>口径与 api/ 一致: <b>没有 handler 的路径一律 404</b>(Spring 静态资源处理器返回 SC_NOT_FOUND),
     * 线上验收时 {@code GET /nacos/v1/cs/configs} 必须是 <b>404 且不是 HTML 首页</b>。
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
                        // ⚠ nacos/ = 已下线的 Nacos 兼容面前缀(TASK-20261022-T1)。
                        //   绝不能回落到 index.html: 那样兼容面会以 200 + SPA 首页的形态"诈尸"。
                        if (resourcePath.startsWith("api/") || resourcePath.startsWith("assets/")
                                || resourcePath.startsWith("nacos/")) {
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
                        // ⚠ 原注释（2026-10-22 TASK-20261022-020 · T3 已失效，保留原文备查）：
                        // 「Nacos 兼容面走 nacos-client 自己的 accessToken 机制，与本服务 Cookie token 无关，
                        //   沿用 TokenInterceptor 会把标准 nacos-client 全部挡在门外，等于兼容面失效」
                        // → 该豁免**已删除**：/nacos/** 不再被排除在 TokenInterceptor 之外。
                        // ⚠ 2026-10-22 (TASK-20261022-T1 · 兼容面下线)：整段已**作废** ——
                        //   用户拍板 T1 = 「直接下线」，兼容面**不存在**了，既没有"标准 nacos-client"
                        //   要接进来，也没有"accessToken 通道"要保留 ⇒ 名单里**不可能**再有 /nacos/**。
                        //   下线后兼容面路径的落点是 404(见 addResourceHandlers 的 nacos/ 口径),
                        //   与本拦截器无关；本拦截器名单保持原样(不新增、不删除任何 /nacos 条目)。
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

    /**
     * 已退役路径硬闸：Nacos 兼容面 {@code /nacos/**}（TASK-20261022-T1 · 用户 2026-10-22 拍板 T1 = 直接下线）。
     *
     * <p>注册在 Servlet 过滤器链上 ⇒ 早于 Spring MVC ⇒ 兼容面路径**任何**方法都统一回 404。
     * 控制器已删除（handler 表里没有兼容面），本过滤器只负责把"写方法落到静态资源处理器 ⇒ 405"
     * 这个不一致口径也拉齐到 404，并保证将来有人把控制器加回来时线上行为仍停在 404。
     *
     * @see NacosCompatRetiredPathFilter
     */
    @Bean
    public FilterRegistrationBean<NacosCompatRetiredPathFilter> nacosCompatRetiredPathFilter() {
        FilterRegistrationBean<NacosCompatRetiredPathFilter> registration =
                new FilterRegistrationBean<NacosCompatRetiredPathFilter>(new NacosCompatRetiredPathFilter());
        registration.addUrlPatterns("/nacos", "/nacos/*");
        registration.setName("nacosCompatRetiredPathFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}