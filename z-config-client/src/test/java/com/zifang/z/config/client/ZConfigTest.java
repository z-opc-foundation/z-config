package com.zifang.z.config.client;

import com.zifang.util.core.meta.Result;
import com.zifang.z.config.client.config.ZConfigFactory;
import com.zifang.z.config.client.config.ZConfigService;
import com.zifang.z.config.client.config.listener.ZConfigListener;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 配置中心客户端集成测试。
 *
 * <p><b>门控</b>：需要一个真实可连的注册中心。地址不落代码库——设置环境变量
 * {@code ZCONFIG_IT_NACOS_ADDR}（如 {@code 127.0.0.1:8084}）才执行；未设置时整类跳过。</p>
 *
 * <p><b>注意这些用例会真实读写注册中心</b>（saveConfig 会落库），所以默认不跑。
 * 需要跑时请指向自己的实例，不要指向共享环境。</p>
 *
 * <p>改写记录：原实现末尾是 {@code Thread.sleep(1000000L)}（约 16.7 分钟）且之后不做任何断言，
 * 同时另起一个 {@code while(true)} 的非守护线程，每秒往注册中心写一次配置且永不停止——
 * 单这一项就让本模块每次 {@code mvn test} 多花 17 分钟，并对共享环境持续产生写副作用。
 * 现改为有界等待 + 真实断言，写线程在 finally 里确保退出。</p>
 */
public class ZConfigTest {

    private static final String ADDR_ENV = "ZCONFIG_IT_NACOS_ADDR";
    private static final String DATA_ID = "user-service-dev.yaml";
    private static final String GROUP = "DEFAULT_GROUP";

    /** 监听器最长等待时间：写入线程每 500ms 改一次值，30s 足够覆盖多轮推送。 */
    private static final long LISTENER_TIMEOUT_SECONDS = 30L;

    private String serverAddr;

    @Before
    public void setUp() {
        serverAddr = System.getenv(ADDR_ENV);
        Assume.assumeTrue("需要真实注册中心：请设置环境变量 " + ADDR_ENV + "（如 127.0.0.1:8084）",
                serverAddr != null && !serverAddr.trim().isEmpty());
    }

    /**
     * 8080 是 web 的口子
     * 8888 是 netty 通道
     */
    @Test
    public void test() {
        ZConfigService zConfigService = newConfigService();

        Result<String> saveResponse = zConfigService.saveConfig(GROUP, DATA_ID, "test");
        assertTrue("saveConfig 应成功，实际: " + saveResponse.getMessage(), saveResponse.isSuccess());

        Result<String> configContentResult = zConfigService.getConfig(GROUP, DATA_ID, 5000); // 超时时间5秒
        assertTrue("getConfig 应成功，实际: " + configContentResult.getMessage(),
                configContentResult.isSuccess());
        assertNotNull("getConfig 返回的配置内容不应为 null", configContentResult.getData());
    }

    @Test
    public void testListener() throws Exception {
        final ZConfigService zConfigService = newConfigService();

        Result<String> configContent = zConfigService.getConfig(GROUP, DATA_ID, 5000);
        assertTrue("getConfig 应成功，实际: " + configContent.getMessage(), configContent.isSuccess());

        final CountDownLatch received = new CountDownLatch(1);
        final AtomicReference<String> got = new AtomicReference<>();
        zConfigService.addListener(GROUP, DATA_ID, new ZConfigListener() {
            @Override
            public void receiveConfigInfo(String newConfig) {
                got.set(newConfig);
                received.countDown();
            }
        });

        // 有界地改配置：原来是无尽 while(true)，线程永不退出且持续写注册中心
        final String marker = "listener-probe-" + System.currentTimeMillis();
        Thread writer = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 30; i++) {
                    zConfigService.saveConfig(GROUP, DATA_ID, marker + "-" + i);
                    try {
                        Thread.sleep(500L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }, "zconfig-test-writer");
        writer.setDaemon(true);
        writer.start();

        try {
            assertTrue("注册中心应在 " + LISTENER_TIMEOUT_SECONDS + "s 内推送配置变更",
                    received.await(LISTENER_TIMEOUT_SECONDS, TimeUnit.SECONDS));
            assertNotNull("监听器收到的配置内容不应为 null", got.get());
        } finally {
            writer.interrupt();
            writer.join(TimeUnit.SECONDS.toMillis(5));
        }
    }

    private ZConfigService newConfigService() {
        Properties properties = new Properties();
        properties.put("serverAddr", serverAddr);
        properties.put("namespace", "dev");
        return ZConfigFactory.createConfigService(properties);
    }
}
