package com.zifang.z.config.admin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.zifang.z.config")
public class ZConfigApplication {
    private static Logger log = LogManager.getLogger(ZConfigApplication.class);
    public static void main(String[] args) {
        SpringApplication.run(ZConfigApplication.class, args);
    }
}
