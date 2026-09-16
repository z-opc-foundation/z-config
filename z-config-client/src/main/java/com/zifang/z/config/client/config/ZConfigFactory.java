package com.zifang.z.config.client.config;

import com.zifang.util.core.lang.validator.Validator;
import com.zifang.z.config.client.naming.ZNamingService;
import com.zifang.z.config.client.naming.ZNamingServiceImpl;
import com.zifang.z.config.common.Constance;

import java.util.Properties;

public class ZConfigFactory {

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
