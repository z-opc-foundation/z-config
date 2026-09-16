package com.zifang.z.config.client;

import com.zifang.util.core.meta.Result;
import com.zifang.z.config.client.config.ZConfigFactory;
import com.zifang.z.config.client.config.ZConfigService;
import org.junit.Test;

import java.util.Properties;

public class ZNamingTest {

    private static final String serverAddr = "101.37.80.51:8084";

    @Test
    public void register() {

        Properties properties = new Properties();
        properties.put("serverAddr", serverAddr); // Nacos服务地址
        properties.put("namespace", "dev");

        ZConfigService zConfigService = ZConfigFactory.createConfigService(properties);

        String dataId = "user-service-dev.yaml";
        String group = "DEFAULT_GROUP";
        Result<String> saveResponse = zConfigService.saveConfig(group, dataId, "test");

        Result<String> configContentResult = zConfigService.getConfig(group, dataId, 5000); // 超时时间5秒
        if (configContentResult.isSuccess()) {
            System.out.println("=== 配置内容 ===");
            System.out.println(configContentResult.getData());
        }


    }
}
