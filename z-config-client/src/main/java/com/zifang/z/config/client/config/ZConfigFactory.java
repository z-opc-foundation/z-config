package com.zifang.z.config.client.config;

import com.zifang.util.core.lang.validator.Validator;
import com.zifang.z.config.client.naming.ZNamingService;
import com.zifang.z.config.client.naming.ZNamingServiceImpl;
import com.zifang.z.config.common.Constance;

import java.util.Properties;

public class ZConfigFactory {

    /**
     * 创建配置服务客户端
     * <p>
     * serverAddr 支持逗号分隔的多个地址（故障转移），如 "host1:8848,host2:8848,host3:8848"
     */
    public static ZConfigService createConfigService(Properties properties) {

        Validator.requireNonNull(properties.getProperty(Constance.SERVICE_ADDR), "required serverAddr");
        Validator.requireNonNull(properties.getProperty(Constance.NAME_SPACE), "required namespace");

        ZConfigService zConfigService = new ZConfigServiceImpl(
                properties.getProperty(Constance.SERVICE_ADDR),
                properties.getProperty(Constance.NAME_SPACE)
        );

        return zConfigService;
    }

    public static ZNamingService createNamingService(Properties properties) {

        Validator.requireNonNull(properties.getProperty(Constance.SERVICE_ADDR), "required serverAddr");
        Validator.requireNonNull(properties.getProperty(Constance.NAME_SPACE), "required namespace");

        ZNamingService zNamingService = new ZNamingServiceImpl(
                properties.getProperty(Constance.SERVICE_ADDR),
                properties.getProperty(Constance.NAME_SPACE)
        );

        return zNamingService;
    }
}
