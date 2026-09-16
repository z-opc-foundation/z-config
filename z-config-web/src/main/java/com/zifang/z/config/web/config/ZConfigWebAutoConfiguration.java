package com.zifang.z.config.web.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * z-config Web 模块自动配置入口
 * <p>
 * 通过 META-INF/spring.factories 被 Spring Boot 自动加载，扫描本模块全部 bean。
 * main-starter 不需要在 ComponentScan 中列出 z-config 包路径。
 */
@Configuration
@ComponentScan(basePackages = "com.zifang.z.config")
public class ZConfigWebAutoConfiguration {
}
