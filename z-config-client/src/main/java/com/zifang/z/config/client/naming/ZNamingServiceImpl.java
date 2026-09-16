package com.zifang.z.config.client.naming;

import com.zifang.util.core.meta.Result;
import com.zifang.z.config.client.naming.listener.ZNamingListener;
import com.zifang.z.config.client.naming.listener.ZNamingServiceListenerManager;
import com.zifang.z.config.client.support.NamingCallClient;
import com.zifang.z.config.common.model.ZNamingInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Naming服务客户端实现
 * 通过HTTP调用服务端API，而非本地存储
 * <p>
 * 注：使用被 @Deprecated 标记的 HttpRequestProxy，因为 NamingCallClient 是
 * 注解驱动的接口代理（@RestController + @RequestMapping），重构为
 * HttpExecutor 需要重写 11 个方法。后续专项迁移。
 */
@SuppressWarnings("deprecation")
public class ZNamingServiceImpl implements ZNamingService {

    private final String serverHost;
    private final int serverPort;
    private final String namespace;

    private NamingCallClient namingClient;
    private ZNamingServiceListenerManager listenerManager;

    public ZNamingServiceImpl(String serverAddr, String namespace) {
        String[] parts = serverAddr.split(":");
        this.serverHost = parts[0];
        this.serverPort = parts.length > 1 ? Integer.parseInt(parts[1]) : 8888;
        this.namespace = namespace;
        initClient();
    }

    public ZNamingServiceImpl(Properties properties) {
        String serverAddr = properties.getProperty("serverAddr", "127.0.0.1:8888");
        String[] parts = serverAddr.split(":");
        this.serverHost = parts[0];
        this.serverPort = parts.length > 1 ? Integer.parseInt(parts[1]) : 8888;
        this.namespace = properties.getProperty("namespace", "public");
        initClient();
    }

    private void initClient() {
        this.namingClient = new NamingCallClient(serverHost, serverPort);
    }

    /**
     * 初始化监听管理器
     */
    public synchronized void initListenerManager(String consumerIp, int consumerPort, String consumerServiceName) {
        if (this.listenerManager == null) {
            this.listenerManager = new ZNamingServiceListenerManager(
                    serverHost, serverPort, namespace,
                    consumerIp, consumerPort, consumerServiceName);
        }
    }

    @Override
    public void addListener(String serviceName, ZNamingListener listener) {
        if (listenerManager == null) {
            throw new IllegalStateException("监听管理器未初始化，请先调用 initListenerManager()");
        }
        listenerManager.addListener(serviceName, listener);
    }

    @Override
    public void removeListener(String serviceName, ZNamingListener listener) {
        if (listenerManager != null) {
            listenerManager.removeListener(serviceName, listener);
        }
    }

    @Override
    public void registerInstance(String serviceName, String ip, int port) {
        Result<String> result = namingClient.registerInstanceSimple(serviceName, ip, port);
        if (!result.isSuccess()) {
            throw new RuntimeException("注册服务失败: " + result.getMessage());
        }
    }

    @Override
    public void registerInstance(String serviceName, String ip, int port, String clusterName) {
        Result<String> result = namingClient.registerInstanceWithCluster(serviceName, ip, port, clusterName);
        if (!result.isSuccess()) {
            throw new RuntimeException("注册服务失败: " + result.getMessage());
        }
    }

    @Override
    public void deregisterInstance(String serviceName, String ip, int port) {
        Result<String> result = namingClient.deregisterInstanceSimple(serviceName, ip, port);
        if (!result.isSuccess()) {
            throw new RuntimeException("注销服务失败: " + result.getMessage());
        }
    }

    @Override
    public void deregisterInstance(String serviceName, String ip, int port, String clusterName) {
        Result<String> result = namingClient.deregisterInstanceWithCluster(serviceName, ip, port, clusterName);
        if (!result.isSuccess()) {
            throw new RuntimeException("注销服务失败: " + result.getMessage());
        }
    }

    @Override
    public List<ZNamingInstance> getAllInstances(String serviceName) {
        Result<List<ZNamingInstance>> result = namingClient.getAllInstances(serviceName, "DEFAULT_GROUP", namespace);
        return result.isSuccess() && result.getData() != null ? result.getData() : new ArrayList<>();
    }

    @Override
    public List<ZNamingInstance> selectInstances(String serviceName, boolean healthy) {
        Result<List<ZNamingInstance>> result = namingClient.selectInstances(serviceName, healthy, null);
        return result.isSuccess() && result.getData() != null ? result.getData() : new ArrayList<>();
    }

    @Override
    public List<ZNamingInstance> selectInstances(String serviceName, String clusterName, boolean healthy) {
        Result<List<ZNamingInstance>> result = namingClient.selectInstances(serviceName, healthy, clusterName);
        return result.isSuccess() && result.getData() != null ? result.getData() : new ArrayList<>();
    }

    @Override
    public ZNamingInstance selectOneHealthyInstance(String serviceName) {
        Result<ZNamingInstance> result = namingClient.selectOneHealthyInstance(serviceName);
        return result.isSuccess() ? result.getData() : null;
    }
}