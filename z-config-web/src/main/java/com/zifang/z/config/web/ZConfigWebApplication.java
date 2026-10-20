package com.zifang.z.config.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * z-config-web 可执行入口。
 *
 * <p>本模块同时是「被 starter 依赖的暴露库」和「自己跑的服务端进程」，两种形态共用一个 jar：
 * repackage 带 classifier=exec，原 jar 保持普通库形态供 z-config-spring-boot-starter 依赖，
 * 可执行产物落在 {@code z-config-web-{revision}-exec.jar}。
 */
@SpringBootApplication(scanBasePackages = "com.zifang.z.config")
public class ZConfigWebApplication {

    private static final Logger log = LogManager.getLogger(ZConfigWebApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ZConfigWebApplication.class, args);
        log.info("z-config-web started");
    }
}
