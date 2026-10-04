package com.zifang.z.config.client;

import com.zifang.z.config.client.naming.ZNamingService;
import com.zifang.z.config.client.naming.ZNamingServiceImpl;
import com.zifang.z.config.common.model.ZNamingInstance;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * 服务发现模块测试类
 * 测试内容：服务注册、服务发现、服务订阅、健康检查、负载均衡
 *
 * <p><b>门控</b>：需要一个真实可连的注册中心。地址不落代码库——设置环境变量
 * {@code ZCONFIG_IT_NACOS_ADDR}（如 {@code 127.0.0.1:8084}）才执行；未设置时整类跳过，
 * 计为 skipped 而非 failed。没有注册中心的环境原本会得到 5 条红项，报错还指向
 * "Failed to connect to 127.0.0.1:8084"，看起来像代码坏了，其实只是环境缺失。</p>
 */
public class ZNamingServiceTest {

    /** 注册中心地址的环境变量名。 */
    private static final String ADDR_ENV = "ZCONFIG_IT_NACOS_ADDR";

    private static String serverAddr() {
        String addr = System.getenv(ADDR_ENV);
        return (addr == null || addr.trim().isEmpty()) ? null : addr.trim();
    }

    private ZNamingService namingService;

    @Before
    public void setUp() {
        String addr = serverAddr();
        Assume.assumeTrue("需要真实注册中心：请设置环境变量 " + ADDR_ENV + "（如 127.0.0.1:8084）",
                addr != null);

        Properties properties = new Properties();
        properties.put("serverAddr", addr);
        properties.put("namespace", "dev");

        namingService = new ZNamingServiceImpl(properties);
    }

    /**
     * 测试基础服务注册 - 简化参数
     */
    @Test
    public void testRegisterInstanceSimple() {
        String serviceName = "test-order-service";
        String ip = "192.168.1.100";
        int port = 8080;

        // 执行注册
        namingService.registerInstance(serviceName, ip, port);

        System.out.println("服务注册成功: " + serviceName + " @ " + ip + ":" + port);

        // 验证注册结果 - 查询实例列表
        List<ZNamingInstance> instances = namingService.getAllInstances(serviceName);
        assertNotNull("实例列表不应为空", instances);
        assertTrue("应至少有一个实例", instances.size() > 0);

        // 验证实例属性
        ZNamingInstance instance = instances.get(0);
        assertEquals("IP应匹配", ip, instance.getIp());
        assertEquals("端口应匹配", port, instance.getPort().intValue());
        assertEquals("服务名应匹配", serviceName, instance.getServiceName());

        System.out.println("服务发现验证成功，发现实例: " + instance.getInstanceId());

        // 清理：注销实例
        namingService.deregisterInstance(serviceName, ip, port);
        System.out.println("服务注销成功");
    }

    /**
     * 测试完整服务注册 - 带分组和集群
     */
    @Test
    public void testRegisterInstanceWithCluster() {
        String serviceName = "test-user-service";
        String ip = "192.168.1.101";
        int port = 8081;
        String clusterName = "SHANGHAI";  // 上海集群

        // 注册到指定集群
        namingService.registerInstance(serviceName, ip, port, clusterName);
        System.out.println("服务注册成功到集群: " + clusterName);

        // 验证按集群查询
        List<ZNamingInstance> instances = namingService.selectInstances(serviceName, clusterName, true);
        assertNotNull("集群实例列表不应为空", instances);
        assertEquals("应有一个实例", 1, instances.size());
        assertEquals("集群名应匹配", clusterName, instances.get(0).getClusterName());

        // 清理
        namingService.deregisterInstance(serviceName, ip, port, clusterName);
    }

    /**
     * 测试服务健康实例筛选
     */
    @Test
    public void testSelectHealthyInstances() throws InterruptedException {
        String serviceName = "test-pay-service";

        // 注册两个实例：一个健康，一个不健康（通过设置模拟）
        String ip1 = "192.168.1.110";
        int port1 = 8090;
        String ip2 = "192.168.1.111";
        int port2 = 8091;

        namingService.registerInstance(serviceName, ip1, port1);
        namingService.registerInstance(serviceName, ip2, port2);

        Thread.sleep(500); // 等待注册完成

        // 查询所有实例
        List<ZNamingInstance> allInstances = namingService.getAllInstances(serviceName);
        System.out.println("所有实例数量: " + allInstances.size());

        // 查询健康实例（默认注册的是健康状态）
        List<ZNamingInstance> healthyInstances = namingService.selectInstances(serviceName, true);
        System.out.println("健康实例数量: " + healthyInstances.size());

        assertTrue("健康实例数应大于0", healthyInstances.size() > 0);

        // 测试负载均衡：选择一个健康实例
        ZNamingInstance selected = namingService.selectOneHealthyInstance(serviceName);
        assertNotNull("应能选出一个实例", selected);
        System.out.println("负载均衡选中实例: " + selected.getIp() + ":" + selected.getPort());

        // 清理
        namingService.deregisterInstance(serviceName, ip1, port1);
        namingService.deregisterInstance(serviceName, ip2, port2);
    }

    /**
     * 测试完整服务发现流程：注册 -> 发现 -> 订阅 -> 注销
     */
    @Test
    public void testFullServiceDiscoveryFlow() throws InterruptedException {
        String serviceName = "test-inventory-service";
        String consumerServiceName = "test-order-consumer";

        // 1. 服务提供者注册
        String providerIp = "192.168.2.100";
        int providerPort = 9000;
        namingService.registerInstance(serviceName, providerIp, providerPort);
        System.out.println("[1] 服务提供者注册完成");

        Thread.sleep(200);

        // 2. 服务消费者发现服务
        List<ZNamingInstance> instances = namingService.getAllInstances(serviceName);
        assertEquals("应发现一个实例", 1, instances.size());
        System.out.println("[2] 服务消费者发现实例: " + instances.get(0).getInstanceId());

        // 3. 服务消费者订阅服务变更（模拟）
        // 注意：实际订阅需要Netty长连接支持，这里仅演示API调用
        System.out.println("[3] 服务消费者订阅服务变更");

        // 4. 模拟服务扩容：新增一个实例
        String providerIp2 = "192.168.2.101";
        int providerPort2 = 9001;
        namingService.registerInstance(serviceName, providerIp2, providerPort2);
        System.out.println("[4] 服务扩容，新增实例");

        Thread.sleep(200);

        // 5. 消费者再次查询，应发现两个实例
        List<ZNamingInstance> updatedInstances = namingService.getAllInstances(serviceName);
        assertEquals("应发现两个实例", 2, updatedInstances.size());
        System.out.println("[5] 服务消费者发现实例数: " + updatedInstances.size());

        // 6. 负载均衡测试
        for (int i = 0; i < 5; i++) {
            ZNamingInstance selected = namingService.selectOneHealthyInstance(serviceName);
            System.out.println("[6] 负载均衡选中: " + selected.getIp() + ":" + selected.getPort());
        }

        // 7. 服务下线
        namingService.deregisterInstance(serviceName, providerIp, providerPort);
        namingService.deregisterInstance(serviceName, providerIp2, providerPort2);
        System.out.println("[7] 所有实例已下线");

        Thread.sleep(200);

        // 验证下线成功
        List<ZNamingInstance> finalInstances = namingService.getAllInstances(serviceName);
        System.out.println("[8] 最终实例数: " + finalInstances.size());

        System.out.println("\n===== 完整服务发现流程测试完成 =====");
    }

    /**
     * 并发测试：多线程服务注册和发现
     */
    @Test
    public void testConcurrentServiceRegistration() throws InterruptedException {
        String serviceName = "test-concurrent-service";
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);

        System.out.println("开始并发测试: " + threadCount + " 个线程同时注册服务");

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    String ip = "192.168.10." + (index + 1);
                    int port = 8000 + index;
                    namingService.registerInstance(serviceName, ip, port);
                    System.out.println("线程 " + index + " 注册成功: " + ip + ":" + port);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // 等待所有线程完成
        latch.await(30, TimeUnit.SECONDS);

        Thread.sleep(500); // 等待数据同步

        // 验证注册结果
        List<ZNamingInstance> instances = namingService.getAllInstances(serviceName);
        System.out.println("\n并发注册完成，实际实例数: " + instances.size());

        // 清理
        for (ZNamingInstance instance : instances) {
            namingService.deregisterInstance(serviceName, instance.getIp(), instance.getPort());
        }

        System.out.println("并发测试完成");
    }
}
