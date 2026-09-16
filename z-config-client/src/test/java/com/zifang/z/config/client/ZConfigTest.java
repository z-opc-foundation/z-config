package com.zifang.z.config.client;

import com.zifang.util.core.meta.Result;
import com.zifang.z.config.client.config.ZConfigFactory;
import com.zifang.z.config.client.config.ZConfigService;
import com.zifang.z.config.client.config.listener.ZConfigListener;
import org.junit.Test;

import java.util.Properties;

public class ZConfigTest {

    private static final String serverAddr = "101.37.80.51:8084";
//    private static final String serverAddr = "127.0.0.1:8084";

    /**
     * 8080 是web的口子
     * 8888 是netty通道
     */
    @Test
    public void test() {
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

    @Test
    public void testListener() throws InterruptedException {
        Properties properties = new Properties();
        properties.put("serverAddr", serverAddr); // Nacos服务地址
        properties.put("namespace", "dev");

        ZConfigService zConfigService = ZConfigFactory.createConfigService(properties);

        String dataId = "user-service-dev.yaml";
        String group = "DEFAULT_GROUP";

        Result<String> configContent = zConfigService.getConfig(group, dataId, 5000); // 超时时间5秒
        System.out.println("=== 配置内容 ===");
        System.out.println(configContent.getData());

        // 不断变更服务上的数据
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    String config = System.currentTimeMillis() + "";
                    System.out.println("start change config :" + config);
                    zConfigService.saveConfig(group, dataId, config);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();

        zConfigService.addListener(group, dataId, new ZConfigListener() {
            @Override
            public void receiveConfigInfo(String newConfig) {
                System.out.println("得到新的配置:" + newConfig);
            }
        });

        Thread.sleep(1000000L);
    }
}
